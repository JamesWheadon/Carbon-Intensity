import json
from datetime import timedelta

from src.app import create_app
from src.scheduler import Scheduler, check_action_timestamps


def test_charge_time_calls_scheduler_for_action():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-28T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": "2024-09-28T19:00:00"}


def test_charge_time_uses_end_timestamp_as_upper_limit_if_received():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-28T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&end=2024-09-28T18:36:00", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": "2024-09-28T18:00:00"}


def test_charge_time_uses_duration_for_action_time():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-28T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&duration=75", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": "2024-09-28T20:30:00"}


def test_charge_time_returns_not_found_when_out_of_range():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-30T17:59:00", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data for time slot"}


def test_charge_time_returns_not_found_when_before_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-25T17:59:00", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data for time slot"}


def test_charge_time_returns_not_found_when_no_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()

    response = tester.get("/charge-time?current=2024-09-25T17:59:00", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data for time slot"}


def test_charge_time_returns_bad_request_when_before_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&end=2024-09-28T17:36:00", content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "End must be after current"}


def test_intensities_accepts_json_body_and_calculates_schedules():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert fake.intensities_called_with == [266, 312] * 24


def test_intensities_returns_bad_request_when_too_few_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266] * 47,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "too short" in response.get_json()["error"]
    assert fake.intensities_called_with == []


def test_intensities_returns_bad_request_when_too_many_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266] * 49,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "too long" in response.get_json()["error"]
    assert fake.intensities_called_with == []


def test_intensities_returns_bad_request_when_invalid_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": ["256"] * 48,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'256' is not of type 'integer'"}
    assert fake.intensities_called_with == []


def test_intensities_returns_bad_request_when_no_intensities_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'intensities' is a required property"}
    assert fake.intensities_called_with == []


def test_intensities_returns_bad_request_when_date_is_invalid():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-2T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "does not match '^\\\\d{4}-\\\\d{2}-\\\\d{2}T\\\\d{2}:\\\\d{2}:\\\\d{2}$'" in response.get_json()["error"]
    assert fake.intensities_called_with == []


def test_intensities_returns_bad_request_when_no_date_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'date' is a required property"}
    assert fake.intensities_called_with == []


class TestScheduler(Scheduler):
    __test__ = False

    def __init__(self):
        super().__init__()
        self.intensities_called_with = []
        self.durations = [2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20]

    def calculate_schedules(self, intensities, intensities_date):
        self.intensities_called_with = intensities
        self.intensities_date = intensities_date

    def best_action_for(self, timestamp, duration, end_timestamp=None):
        check_action_timestamps(end_timestamp, timestamp)
        if self.intensities_date is None or timestamp < self.intensities_date:
            return None
        action_index = self.action_index_from_timestamp(timestamp)
        end_action_index = min(self.action_index_from_timestamp(end_timestamp), 95) if end_timestamp is not None else 95
        if action_index >= 96:
            return None
        print(duration, duration == 5)
        if duration == 5:
            i = 11
        else:
            i = 5
        return self.intensities_date + timedelta(seconds =min(action_index + i, end_action_index - duration) * 900)
