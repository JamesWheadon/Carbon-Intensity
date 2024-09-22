from src.app import create_app


def test_charge_time_calls_scheduler_for_action():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("/charge-time?current=18", content_type="application/json")

    assert response.status_code == 200
    assert response.data == b'{"chargeTime":21}\n'
    assert fake.time_slots_called_by == [18]


def test_charge_time_returns_not_found_when_out_of_range():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("/charge-time?current=49", content_type="application/json")

    assert response.status_code == 404
    assert response.data == b'{"error":"no data for time slot"}\n'


class TestScheduler:
    __test__ = False
    def __init__(self):
        self.time_slots_called_by = []

    def best_action_for(self, time_slot):
        self.time_slots_called_by.append(time_slot)
        if time_slot == 49:
            return None
        return time_slot + 3
