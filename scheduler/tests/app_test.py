from src.app import create_app


def test_charge_time_calls_scheduler_for_action():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("/charge-time?current=18", content_type="application/json")

    assert response.status_code == 200
    assert response.data == b'{"chargeTime":21}\n'
    assert fake.time_slots_called_by == [18]


class TestScheduler:
    __test__ = False
    def __init__(self):
        self.time_slots_called_by = []

    def best_action_for(self, time_slot):
        self.time_slots_called_by.append(time_slot)
        return time_slot + 3
