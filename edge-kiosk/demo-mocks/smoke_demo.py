"""HTTP acceptance checks against two clean, running Edge lanes and demo mocks.

Use separate empty Edge/mock databases for this run; it leaves test transactions.
Start IN at 8090, OUT at 8091, mocks at 4101..4103, then run this file.
"""
import json
import time
import urllib.request
import uuid

IN = "http://127.0.0.1:8090/api/v1/kiosk"
OUT = "http://127.0.0.1:8091/api/v1/kiosk"


def call(base, path, body=None):
    request = urllib.request.Request(base + path, data=None if body is None else json.dumps(body).encode(),
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=10) as response:
        value = json.load(response)
    return value["data"] if isinstance(value, dict) and "success" in value else value


def wait_for(check, message, timeout=20):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if check():
            return
        time.sleep(0.25)
    raise AssertionError(message)


def mode(service, value):
    call(IN, f"/demo/mocks/{service}", {"mode": value})


def scan(base, kind="VALID"):
    sample = call(base, f"/demo/samples/{kind}")
    request = dict(scanId=str(uuid.uuid4()), payload=sample["payload"])
    return call(base, "/scans", request), request


def settled(base):
    wait_for(lambda: call(base, "/status")["pendingEvents"] == 0, "Outbox did not receive ACK", timeout=75)
    # Registry outcome delivery runs before sync in each worker tick.


def exit_lane():
    tx, _ = scan(OUT)
    assert tx["state"] == "OPEN_CONFIRMED", tx
    settled(OUT)


def main():
    for service in ("registry", "turnstile", "core"):
        mode(service, "NORMAL")
    for kind in ("EXPIRED", "BAD_SIGNATURE", "WRONG_FACTORY", "OUTSIDE_SHIFT", "INACTIVE"):
        tx, _ = scan(IN, kind)
        assert tx["state"] == "DENIED", tx
    settled(IN)

    tx, request = scan(IN)
    assert tx["state"] == "OPEN_CONFIRMED", tx
    assert call(IN, "/scans", request)["transactionId"] == tx["transactionId"]
    replay = call(IN, "/scans", dict(request, scanId=str(uuid.uuid4())))
    assert replay["state"] == "DENIED" and replay["reason"] == "NONCE_ALREADY_USED", replay
    settled(IN)
    exit_lane()
    print("PASS validation, request idempotency, nonce replay and shared IN/OUT", flush=True)

    mode("core", "DOWN")
    tx, _ = scan(IN)
    assert tx["state"] == "OPEN_CONFIRMED", tx
    assert call(IN, "/status")["pendingEvents"] > 0
    mode("core", "NORMAL")
    settled(IN)
    exit_lane()
    print("PASS WAN outage and eventual ACK", flush=True)

    mode("turnstile", "LOST_ACK")
    before = call("http://127.0.0.1:4102", "/demo/state")["openCount"]
    tx, _ = scan(IN)
    assert tx["state"] == "OPEN_CONFIRMED", tx
    assert call("http://127.0.0.1:4102", "/demo/state")["openCount"] == before + 1
    mode("turnstile", "NORMAL")
    settled(IN)
    exit_lane()
    print("PASS lost Turnstile ACK with exactly one opening", flush=True)

    mode("turnstile", "UNKNOWN")
    tx, _ = scan(IN)
    assert tx["state"] == "OPEN_UNKNOWN", tx
    before = call("http://127.0.0.1:4102", "/demo/state")["openCount"]
    time.sleep(2.5)
    assert call("http://127.0.0.1:4102", "/demo/state")["openCount"] == before
    call(IN, f"/demo/transactions/{tx['transactionId']}/resolve", {"outcome": "OPEN_CONFIRMED"})
    mode("turnstile", "NORMAL")
    settled(IN)
    assert next(item for item in call(IN, "/status")["transactions"] if item["transactionId"] == tx["transactionId"])["state"] == "OPEN_CONFIRMED"
    exit_lane()
    print("PASS UNKNOWN reservation, no blind retry and reconciliation event", flush=True)

    mode("registry", "LOST_ACK")
    tx, _ = scan(IN)
    assert tx["state"] == "PENDING_AUTHORIZATION", tx
    mode("registry", "NORMAL")
    wait_for(lambda: next(item for item in call(IN, "/status")["transactions"] if item["transactionId"] == tx["transactionId"])["state"] == "OPEN_CONFIRMED", "Authorization did not recover")
    settled(IN)
    exit_lane()
    print("PASS ambiguous Registry authorization resumes same transaction", flush=True)

    mode("registry", "DOWN")
    # Use a fresh employee: this kiosk cannot learn another kiosk's OUT while offline.
    tx, _ = scan(IN, "VALID_SECOND_EMPLOYEE")
    assert tx["state"] == "OPEN_CONFIRMED" and tx["verificationMode"] == "OFFLINE_UNVERIFIED", tx
    mode("registry", "NORMAL")
    settled(IN)
    print("PASS local fallback retains OFFLINE_UNVERIFIED", flush=True)
    print("All HTTP acceptance checks passed", flush=True)


if __name__ == "__main__":
    try:
        main()
    finally:
        for service in ("registry", "turnstile", "core"):
            try:
                mode(service, "NORMAL")
            except Exception:
                pass
