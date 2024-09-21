from flask import Flask

from src.scheduler import UseTimeScheduler


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/")
    def hello():
        return "Hello, World!"

    @app.route("/charge-time")
    def charge_time():
        return "15"

    return app


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run()
