import json
from datetime import timedelta

from src.app import create_app
from src.scheduler import Scheduler


def test_charge_time_calls_scheduler_for_action():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-28T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    tester.patch("/intensities/train?duration=30", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": "2024-09-28T19:15:00"}


def test_charge_time_uses_end_timestamp_as_upper_limit_if_received():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-28T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    tester.patch("/intensities/train?duration=30", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&end=2024-09-28T18:36:00",
                          content_type="application/json")

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
    tester.patch("/intensities/train?duration=75", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&duration=75", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == {"chargeTime": "2024-09-28T20:45:00"}


def test_charge_time_returns_not_found_when_after_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    tester.patch("/intensities/train?duration=30", content_type="application/json")

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
    tester.patch("/intensities/train?duration=30", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-25T17:59:00", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No data for time slot"}


def test_charge_time_returns_not_found_when_duration_not_trained():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-25T17:59:00", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "Duration has not been trained"}


def test_charge_time_returns_bad_request_when_end_before_start():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    tester.patch("/intensities/train?duration=30", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-28T17:59:00&end=2024-09-28T17:36:00",
                          content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "End must be after current plus duration"}


def test_charge_time_returns_bad_request_when_end_before_start_plus_duration():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")
    tester.patch("/intensities/train?duration=30", content_type="application/json")

    response = tester.get("/charge-time?current=2024-09-26T17:59:00&end=2024-09-26T18:28:00",
                          content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "End must be after current plus duration"}


def test_intensities_accepts_json_body():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert fake.get_intensities()["intensities"] == [266, 312] * 24


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
    assert fake.get_intensities() is None


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
    assert fake.get_intensities() is None


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
    assert fake.get_intensities() is None


def test_intensities_returns_bad_request_when_no_intensities_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'intensities' is a required property"}
    assert fake.get_intensities() is None


def test_intensities_returns_bad_request_when_date_is_invalid():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-2T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "does not match '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$'" in response.get_json()["error"]
    assert fake.get_intensities() is None


def test_intensities_returns_bad_request_when_no_date_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'date' is a required property"}
    assert fake.get_intensities() is None


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
    assert fake.get_intensities()["intensities"] == [266, 312] * 24


def test_multi_day_intensities_returns_bad_request_when_too_few_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266] * 47,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "too short" in response.get_json()["error"]
    assert fake.get_intensities() is None


def test_multi_day_intensities_returns_bad_request_when_too_many_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266] * 49,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "too long" in response.get_json()["error"]
    assert fake.get_intensities() is None


def test_multi_day_intensities_returns_bad_request_when_invalid_intensities():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": ["256"] * 48,
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'256' is not of type 'integer'"}
    assert fake.get_intensities() is None


def test_multi_day_intensities_returns_bad_request_when_no_intensities_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "date": "2024-09-26T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'intensities' is a required property"}
    assert fake.get_intensities() is None


def test_multi_day_intensities_returns_bad_request_when_date_is_invalid():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-2T01:00:00"
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert "does not match '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$'" in response.get_json()["error"]
    assert fake.get_intensities() is None


def test_multi_day_intensities_returns_bad_request_when_no_date_in_input():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24
    }

    response = tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "'date' is a required property"}
    assert fake.get_intensities() is None


def test_intensities_returns_intensities_and_date():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.get("/intensities", content_type="application/json")

    assert response.status_code == 200
    assert response.get_json() == test_data


def test_intensities_returns_not_found_with_no_intensities_set():
    fake = TestScheduler()
    tester = create_app(fake).test_client()

    response = tester.get("/intensities", content_type="application/json")

    assert response.status_code == 404
    assert response.get_json() == {"error": "No intensity data for scheduler"}


def test_training_trains_for_duration():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.patch("/intensities/train?duration=60", content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert 4 in fake.durations_trained


def test_training_bad_request_if_no_training_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()

    response = tester.patch("/intensities/train?duration=60", content_type="application/json")

    assert response.status_code == 400
    assert response.get_json() == {"error": "No intensity data for scheduler"}
    assert fake.durations_trained == []


def test_training_trains_for_closest_duration_to_request():
    fake = TestScheduler()
    tester = create_app(fake).test_client()
    test_data = {
        "intensities": [266, 312] * 24,
        "date": "2024-09-26T01:00:00"
    }
    tester.post("/intensities", data=json.dumps(test_data), content_type="application/json")

    response = tester.patch("/intensities/train?duration=500", content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None
    assert fake.durations_trained == [20]


def test_delete_data():
    fake = TestScheduler()
    tester = create_app(fake).test_client()

    response = tester.delete("/intensities", content_type="application/json")

    assert response.status_code == 204
    assert response.get_json() is None


class TestScheduler(Scheduler):
    __test__ = False

    def __init__(self):
        super().__init__()

    def train(self, duration):
        if not self.env:
            raise TypeError("No intensity data for scheduler")
        self.durations_trained.append(duration)

    def best_action_for(self, timestamp, duration, end_timestamp=None):
        self.validate_request(timestamp, duration, end_timestamp)
        action_index, latest_action_index = self.indexes_from_start_and_end(timestamp, duration, end_timestamp)
        if action_index is None:
            return None
        if duration == 5:
            i = 11
        else:
            i = 5
        return self.intensities_date + timedelta(seconds=min(action_index + i, latest_action_index) * 900)
