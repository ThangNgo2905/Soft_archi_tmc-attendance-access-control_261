import tempfile
import unittest
import http.client
import json
import threading
from http.server import ThreadingHTTPServer
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from server import Conflict, State, handler_for


class MockContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.state = State(str(Path(self.temp.name) / "mock.db"))

    def tearDown(self):
        self.state.db.close()
        self.temp.cleanup()

    def authorize(self, tx="T1", nonce="N1", direction="IN"):
        request = dict(transactionId=tx, factoryId="BD", employeeId="E1", nonce=nonce, direction=direction)
        with self.state.lock, self.state.db:
            return self.state.authorize(request)

    def test_authorization_is_idempotent_and_nonce_is_one_time(self):
        first = self.authorize()
        self.assertEqual(self.authorize(), first)
        self.assertEqual(self.authorize("T2")["reason"], "NONCE_ALREADY_USED")
        with self.assertRaises(Conflict):
            self.authorize("T1", "DIFFERENT")

    def test_two_kiosks_cannot_concurrently_consume_same_nonce(self):
        with ThreadPoolExecutor(max_workers=2) as pool:
            decisions = list(pool.map(lambda tx: self.authorize(tx)["decision"], ["T1", "T2"]))
        self.assertCountEqual(decisions, ["ALLOWED", "DENIED"])

    def test_unknown_holds_reservation_and_confirmed_result_changes_direction(self):
        self.authorize()
        with self.state.db:
            self.state.registry_outcome(dict(transactionId="T1", state="OPEN_UNKNOWN"))
        self.assertEqual(self.authorize("T2", "N2", "OUT")["reason"], "ANTI_PASSBACK")
        with self.state.db:
            self.state.registry_outcome(dict(transactionId="T1", state="OPEN_CONFIRMED"))
        self.assertEqual(self.authorize("T3", "N3", "IN")["reason"], "ANTI_PASSBACK")
        self.assertEqual(self.authorize("T4", "N4", "OUT")["decision"], "ALLOWED")

    def test_turnstile_opens_once_for_same_command(self):
        command = dict(transactionId="T1", gateId="G1")
        with self.state.db:
            first = self.state.open_gate(command)
            self.assertEqual(self.state.open_gate(command), first)
        self.assertEqual(self.state.inspect("turnstile")["openCount"], 1)
        with self.assertRaises(Conflict):
            self.state.open_gate(dict(transactionId="T1", gateId="G2"))

    def test_core_deduplicates_but_accepts_later_reconciliation_even_out_of_order(self):
        first = dict(eventId="EV1", transactionId="T1", sequence=2, state="OPEN_UNKNOWN")
        final = dict(eventId="EV2", transactionId="T1", sequence=3, state="OPEN_CONFIRMED")
        with self.state.db:
            for event in [final, first, final]:
                self.state.receive(dict(events=[event]))
        self.assertEqual(self.state.inspect("core")["eventCount"], 2)
        with self.assertRaises(Conflict), self.state.db:
            self.state.receive(dict(events=[dict(first, state="DENIED")]))

    def test_partial_ack_only_persists_acknowledged_events(self):
        self.state.modes["core"] = "PARTIAL_ACK"
        events = [dict(eventId=f"EV{i}", transactionId=f"T{i}", sequence=2) for i in range(4)]
        with self.state.db:
            result = self.state.receive(dict(events=events))
        self.assertEqual(len(result["acknowledgedEventIds"]), 2)
        self.assertEqual(self.state.inspect("core")["eventCount"], 2)

    def test_mock_state_survives_restart(self):
        self.authorize()
        self.state.db.close()
        self.state = State(str(Path(self.temp.name) / "mock.db"))
        self.assertEqual(self.authorize("T2")["reason"], "NONCE_ALREADY_USED")

    def test_chunked_http_post_from_java_style_streaming_client(self):
        server = ThreadingHTTPServer(("127.0.0.1", 0), handler_for("registry", self.state))
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        connection = http.client.HTTPConnection("127.0.0.1", server.server_port)
        try:
            connection.request("POST", "/demo/mode", body=iter([b'{"mode":', b'"NORMAL"}']),
                               headers={"Content-Type": "application/json"}, encode_chunked=True)
            response = connection.getresponse()
            self.assertEqual(response.status, 200)
            self.assertEqual(json.loads(response.read())["mode"], "NORMAL")
        finally:
            connection.close()
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == "__main__":
    unittest.main()
