from src.app import create_app


def test_index():
    tester = create_app(TestScheduler()).test_client()
    response = tester.get("/", content_type="html/text")

    assert response.status_code == 200
    assert response.data == b"Hello, World!"


def test_charge_time():
    tester = create_app(TestScheduler()).test_client()
    response = tester.get("/charge-time", content_type="application/json")

    assert response.status_code == 200
    assert response.data == b"15"


class TestScheduler:
    pass
