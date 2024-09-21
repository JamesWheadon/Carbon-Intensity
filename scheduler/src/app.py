from flask import Flask, request

from src.scheduler import UseTimeScheduler


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/charge-time")
    def charge_time():
        current_time = request.args.get("current", type=int)
        best_action = app.config["SCHEDULER"].best_action_for(current_time)
        return {"chargeTime": best_action}

    return app


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run()
