import json
from datetime import date
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
    assert response.get_json() == {"error": "No data for time slot"}


def test_intensities_accepts_json_body_and_calculates_schedules():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26"
    }
    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert fake.intensities_called_with == [266, 312] * 24


def test_intensities_returns_unprocessable_entity_when_incorrect_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 25,
        "date": "2024-09-26"
    }
    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 422
    assert response.get_json() == {"error": "Invalid intensities, should be an array of 48 time slots"}
    assert fake.intensities_called_with == []


def test_intensities_date_returns_date_of_latest_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    response = tester.get("intensities/date")

    assert response.status_code == 200
    assert response.get_json() == {"date": "2024-09-26"}


def test_intensities_date_returns_404_when_no_data_submitted():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    response = tester.get("intensities/date")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data has been submitted to the scheduler"}


class TestScheduler:
    __test__ = False

    def __init__(self):
        self.time_slots_called_by = []
        self.intensities_called_with = []
        self.intensities_date = None

    def best_action_for(self, time_slot):
        self.time_slots_called_by.append(time_slot)
        if time_slot == 49:
            return None
        return np.int64(time_slot + 3)

    def calculate_schedules(self, intensities, intensities_date):
        self.intensities_called_with = intensities
        self.intensities_date = date.fromisoformat(intensities_date)

    def day_of_data(self):
        return self.intensities_date
