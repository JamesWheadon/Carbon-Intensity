import json
from datetime import timedelta

from src.app import create_app


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


def test_intensities_date_returns_date_of_latest_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("intensities/date")

    assert response.status_code == 200
    assert response.get_json() == {"date": "2024-09-26T01:00:00"}


def test_intensities_date_returns_404_when_no_data_submitted():
    fake = TestScheduler()
    tester = create_app(fake).test_client()

    response = tester.get("intensities/date")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data has been submitted to the scheduler"}


class TestScheduler:
    __test__ = False

    def __init__(self):
        self.intensities_called_with = []
        self.intensities_date = None

    def best_action_for(self, timestamp):
        if self.intensities_date is None or timestamp < self.intensities_date:
            return None
        minutes_diff = (timestamp - self.intensities_date).total_seconds() / 60.0
        action_index = minutes_diff // 30
        if action_index >= 48:
            return None
        return self.intensities_date + timedelta(seconds = (action_index + 3) * 1800)

    def calculate_schedules(self, intensities, intensities_date):
        self.intensities_called_with = intensities
        self.intensities_date = intensities_date

    def day_of_data(self):
        return self.intensities_date
