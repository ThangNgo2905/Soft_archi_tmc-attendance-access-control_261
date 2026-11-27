"""Local-only contract simulators. Three ports let WAN and LAN fail independently.

Python standard library only. The SQLite state survives mock restarts; this is not a
replicated Registry or a production Core implementation.
"""
import argparse
import json
import os
import sqlite3
import threading
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


class Conflict(Exception):
    pass


class State:
    def __init__(self, path):
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        self.db = sqlite3.connect(path, check_same_thread=False)
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("PRAGMA synchronous=FULL")
        self.lock = threading.RLock()
        self.modes = {name: "NORMAL" for name in ("registry", "turnstile", "core")}
        self.db.executescript("""
            CREATE TABLE IF NOT EXISTS authorizations(id TEXT PRIMARY KEY, request TEXT NOT NULL, result TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS nonce(factory TEXT,employee TEXT,nonce TEXT,PRIMARY KEY(factory,employee,nonce));
            CREATE TABLE IF NOT EXISTS presence(factory TEXT,employee TEXT,direction TEXT,reservation TEXT,PRIMARY KEY(factory,employee));
            CREATE TABLE IF NOT EXISTS outcomes(id TEXT PRIMARY KEY,state TEXT NOT NULL);
            CREATE TABLE IF NOT EXISTS gates(id TEXT PRIMARY KEY,request TEXT NOT NULL,outcome TEXT NOT NULL,opens INTEGER NOT NULL);
            CREATE TABLE IF NOT EXISTS events(id TEXT PRIMARY KEY,tx TEXT NOT NULL,sequence INTEGER NOT NULL,body TEXT NOT NULL,UNIQUE(tx,sequence));
        """)

    def authorize(self, request):
        tx, factory, employee, nonce, direction = (request[k] for k in
                ("transactionId", "factoryId", "employeeId", "nonce", "direction"))
        if direction not in ("IN", "OUT"):
            raise ValueError("direction")
        existing = self.db.execute("SELECT request,result FROM authorizations WHERE id=?", (tx,)).fetchone()
        if existing:
            if existing[0] != canonical(request):
                raise Conflict("TRANSACTION_ID_CONFLICT")
            return json.loads(existing[1])
        reason = "AUTHORIZED"
        used = self.db.execute("SELECT 1 FROM nonce WHERE factory=? AND employee=? AND nonce=?", (factory, employee, nonce)).fetchone()
        state = self.db.execute("SELECT direction,reservation FROM presence WHERE factory=? AND employee=?", (factory, employee)).fetchone()
        if used:
            reason = "NONCE_ALREADY_USED"
        else:
            self.db.execute("INSERT INTO nonce VALUES(?,?,?)", (factory, employee, nonce))
            if state and (state[1] or state[0] == direction):
                reason = "ANTI_PASSBACK"
            elif self.modes["registry"] == "DENY":
                reason = "REGISTRY_DENIED"
        result = {"decision": "ALLOWED" if reason == "AUTHORIZED" else "DENIED", "reason": reason}
        self.db.execute("INSERT INTO authorizations VALUES(?,?,?)", (tx, canonical(request), canonical(result)))
        if result["decision"] == "ALLOWED":
            self.db.execute("INSERT INTO presence(factory,employee,reservation) VALUES(?,?,?) ON CONFLICT(factory,employee) DO UPDATE SET reservation=excluded.reservation", (factory, employee, tx))
        return result

    def registry_outcome(self, request):
        tx, outcome = request["transactionId"], request["state"]
        if outcome not in ("OPEN_CONFIRMED", "DENIED", "OPEN_UNKNOWN"):
            raise ValueError("state")
        row = self.db.execute("SELECT request,result FROM authorizations WHERE id=?", (tx,)).fetchone()
        if not row or json.loads(row[1])["decision"] != "ALLOWED":
            raise Conflict("AUTHORIZATION_REQUIRED")
        prior = self.db.execute("SELECT state FROM outcomes WHERE id=?", (tx,)).fetchone()
        if prior and prior[0] in ("OPEN_CONFIRMED", "DENIED") and prior[0] != outcome:
            raise Conflict("FINAL_OUTCOME_CONFLICT")
        self.db.execute("INSERT INTO outcomes VALUES(?,?) ON CONFLICT(id) DO UPDATE SET state=excluded.state", (tx, outcome))
        source = json.loads(row[0])
        if outcome == "OPEN_CONFIRMED":
            self.db.execute("UPDATE presence SET direction=?,reservation=NULL WHERE factory=? AND employee=? AND reservation=?", (source["direction"], source["factoryId"], source["employeeId"], tx))
        elif outcome == "DENIED":
            self.db.execute("UPDATE presence SET reservation=NULL WHERE reservation=?", (tx,))
        return True

    def open_gate(self, request):
        tx = request["transactionId"]
        existing = self.db.execute("SELECT request,outcome FROM gates WHERE id=?", (tx,)).fetchone()
        if existing:
            if existing[0] != canonical(request):
                raise Conflict("TRANSACTION_ID_CONFLICT")
            return {"outcome": existing[1]}
        mode = self.modes["turnstile"]
        outcome = "DENIED" if mode == "DENY" else "OPEN_UNKNOWN" if mode == "UNKNOWN" else "OPEN_CONFIRMED"
        # UNKNOWN models a command whose physical result cannot be established.
        self.db.execute("INSERT INTO gates VALUES(?,?,?,?)", (tx, canonical(request), outcome, int(outcome == "OPEN_CONFIRMED")))
        return {"outcome": outcome}

    def receive(self, request):
        events = request["events"]
        if not isinstance(events, list) or len(events) > 100:
            raise ValueError("batch")
        selected = events[:max(1, len(events) // 2)] if self.modes["core"] == "PARTIAL_ACK" else events
        acknowledged = []
        for event in selected:
            row = self.db.execute("SELECT body FROM events WHERE id=? OR (tx=? AND sequence=?)", (event["eventId"], event["transactionId"], event["sequence"])).fetchone()
            if row and row[0] != canonical(event):
                raise Conflict("EVENT_ID_OR_SEQUENCE_CONFLICT")
            if not row:
                self.db.execute("INSERT INTO events VALUES(?,?,?,?)", (event["eventId"], event["transactionId"], event["sequence"], canonical(event)))
            acknowledged.append(event["eventId"])
        return {"acknowledgedEventIds": acknowledged}

    def inspect(self, service):
        return {"service": service, "mode": self.modes[service],
                "openCount": self.db.execute("SELECT COALESCE(SUM(opens),0) FROM gates").fetchone()[0],
                "eventCount": self.db.execute("SELECT COUNT(*) FROM events").fetchone()[0],
                "authorizationCount": self.db.execute("SELECT COUNT(*) FROM authorizations").fetchone()[0]}


def cache_snapshot(factory):
    def employee(employee_id, assigned, active=True, start="00:00:00", end="23:59:59.999999999"):
        return {"employeeId": employee_id, "factoryId": assigned, "keyId": "demo-key-v1",
                "secret": "demo-only-not-a-production-secret", "active": active,
                "shiftStart": start, "shiftEnd": end}
    return {"version": 2, "employees": [employee("DEMO-001", factory),
            employee("DEMO-002", factory, end="00:00:00"), employee("DEMO-003", factory, active=False),
            employee("DEMO-004", factory),
            employee("DEMO-OTHER", "OTHER")]}


def handler_for(service, state):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):
            pass  # Never print QR payloads, employee secrets or event bodies.

        def respond(self, code, body):
            encoded = canonical(body).encode()
            try:
                self.send_response(code)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(encoded)))
                self.end_headers()
                self.wfile.write(encoded)
            except (BrokenPipeError, ConnectionResetError):
                pass

        def do_GET(self):
            self.dispatch(False)

        def do_POST(self):
            self.dispatch(True)

        def read_body(self):
            # Spring's JDK HTTP client may stream JSON using chunked transfer encoding.
            if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
                chunks = bytearray()
                while True:
                    line = self.rfile.readline(128)
                    if not line.endswith(b"\r\n"):
                        raise ValueError("Invalid chunk size")
                    size = int(line.split(b";", 1)[0], 16)
                    if size < 0 or len(chunks) + size > 262144:
                        raise ValueError("Body too large")
                    if size == 0:
                        while self.rfile.readline(8192) not in (b"\r\n", b"", b"\n"):
                            pass
                        return bytes(chunks)
                    data = self.rfile.read(size)
                    if len(data) != size or self.rfile.read(2) != b"\r\n":
                        raise ValueError("Invalid chunk")
                    chunks.extend(data)
            size = int(self.headers.get("Content-Length", 0))
            if size < 0 or size > 262144:
                raise ValueError("Body too large")
            return self.rfile.read(size)

        def dispatch(self, post):
            parsed = urlparse(self.path)
            path = parsed.path
            try:
                body = self.read_body() if post else b""
                request = json.loads(body) if body else {}
                lost_ack = False
                with state.lock:
                    if path == "/demo/state" and not post:
                        result = state.inspect(service)
                    elif path == "/demo/mode" and post:
                        modes = {"registry": {"NORMAL", "DOWN", "DENY", "LOST_ACK"},
                                 "turnstile": {"NORMAL", "DOWN", "DENY", "LOST_ACK", "UNKNOWN"},
                                 "core": {"NORMAL", "DOWN", "LOST_ACK", "PARTIAL_ACK"}}
                        if request["mode"] not in modes[service]:
                            raise ValueError("mode")
                        state.modes[service] = request["mode"]
                        result = state.inspect(service)
                    elif path == "/demo/resolve" and post and service == "turnstile":
                        if request["outcome"] not in ("OPEN_CONFIRMED", "DENIED"):
                            raise ValueError("outcome")
                        with state.db:
                            count = state.db.execute("UPDATE gates SET outcome=?,opens=? WHERE id=? AND outcome='OPEN_UNKNOWN'", (request["outcome"], int(request["outcome"] == "OPEN_CONFIRMED"), request["transactionId"])).rowcount
                            if not count:
                                raise Conflict("UNKNOWN_TRANSACTION_REQUIRED")
                        result = True
                    elif path == "/health":
                        result = {"status": "UP"}
                    elif state.modes[service] == "DOWN":
                        self.respond(503, {"notProcessed": True})
                        return
                    else:
                        with state.db:
                            if service == "registry" and path == "/api/v1/authorizations" and post:
                                result = state.authorize(request)
                                lost_ack = state.modes[service] == "LOST_ACK"
                            elif service == "registry" and path.startswith("/api/v1/authorizations/") and not post:
                                row = state.db.execute("SELECT result FROM authorizations WHERE id=?", (path.rsplit("/", 1)[1],)).fetchone()
                                result = json.loads(row[0]) if row else {"decision": "NOT_FOUND", "reason": "NOT_FOUND"}
                            elif service == "registry" and path == "/api/v1/outcomes" and post:
                                result = state.registry_outcome(request)
                            elif service == "turnstile" and path == "/api/v1/commands" and post:
                                result = state.open_gate(request)
                                lost_ack = state.modes[service] == "LOST_ACK"
                            elif service == "turnstile" and path.startswith("/api/v1/commands/") and not post:
                                row = state.db.execute("SELECT outcome FROM gates WHERE id=?", (path.rsplit("/", 1)[1],)).fetchone()
                                result = {"outcome": row[0] if row else "NOT_FOUND"}
                            elif service == "core" and path == "/api/v1/events/batch" and post:
                                result = state.receive(request)
                                lost_ack = state.modes[service] == "LOST_ACK"
                            elif service == "core" and path == "/api/v1/cache" and not post:
                                result = cache_snapshot(parse_qs(parsed.query).get("factoryId", ["BD"])[0])
                            elif service == "core" and path == "/api/v1/heartbeat" and post:
                                result = True
                            else:
                                self.respond(404, {"error": "NOT_FOUND"})
                                return
                # Commit and release the lock before simulating a lost acknowledgement.
                if lost_ack:
                    time.sleep(1.5)
                self.respond(200, result)
            except Conflict as ex:
                self.respond(409, {"error": str(ex)})
            except (ValueError, KeyError, TypeError):
                self.respond(400, {"error": "INVALID_REQUEST"})
            except sqlite3.Error:
                self.respond(500, {"error": "MOCK_DATABASE_ERROR"})
    return Handler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bind", default="127.0.0.1")
    parser.add_argument("--database", default="data/mock.db")
    parser.add_argument("--base-port", type=int, default=4101)
    args = parser.parse_args()
    state = State(args.database)
    servers = []
    for offset, service in enumerate(("registry", "turnstile", "core")):
        server = ThreadingHTTPServer((args.bind, args.base_port + offset), handler_for(service, state))
        threading.Thread(target=server.serve_forever, daemon=True).start()
        servers.append(server)
        print(f"{service}: http://{args.bind}:{args.base_port + offset}", flush=True)
    try:
        threading.Event().wait()
    except KeyboardInterrupt:
        for server in servers:
            server.shutdown()
        state.db.close()


if __name__ == "__main__":
    main()
