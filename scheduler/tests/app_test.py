import json

import numpy as np

from src.app import create_app


def test_charge_time_calls_scheduler_for_action():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("/charge-time?current=18", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": 21}
    assert fake.time_slots_called_by == [18]


def test_charge_time_returns_not_found_when_out_of_range():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("/charge-time?current=49", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "no data for time slot"}


def test_intensities_accepts_json_body_and_calculates_schedules():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    time_slots = [
                     {"from": "2018-01-20T12:00Z", "to": "2018-01-20T12:30Z", "forecast": 266},
                     {"from": "2018-01-20T12:30Z", "to": "2018-01-20T13:00Z", "forecast": 312}
                 ] * 24
    test_data = {
        "data": time_slots
    }
    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert fake.intensities_called_with == [266, 312] * 24


def test_intensities_returns_unprocessable_entity_when_incorrect_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "data": [
            {
                "from": "2018-01-20T12:00Z",
                "to": "2018-01-20T12:30Z",
                "forecast": 266
            },
            {
                "from": "2018-01-20T12:30Z",
                "to": "2018-01-20T13:00Z",
                "forecast": 312
            }
        ]
    }
    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 422
    assert response.get_json() == {"error": "invalid intensities, should be an array of 48 time slots"}
    assert fake.intensities_called_with == []


class TestScheduler:
    __test__ = False

    def __init__(self):
        self.time_slots_called_by = []
        self.intensities_called_with = []

    def best_action_for(self, time_slot):
        self.time_slots_called_by.append(time_slot)
        if time_slot == 49:
            return None
        return np.int64(time_slot + 3)

    def calculate_schedules(self, intensities):
        self.intensities_called_with = intensities
