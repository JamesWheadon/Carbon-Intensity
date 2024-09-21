from flask import Flask, request

from src.scheduler import UseTimeScheduler


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/")
    def hello():
        return "Hello, World!"

    @app.route("/charge-time")
    def charge_time():
        current_time = int(request.args.get("current"))
        best_action = app.config["SCHEDULER"].best_action_for(current_time)
        return str(best_action)

    return app


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run()
