from flask import Flask, request, Response

from src.scheduler import UseTimeScheduler


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/charge-time")
    def charge_time():
        current_time = request.args.get("current", type=int)
        best_action = app.config["SCHEDULER"].best_action_for(current_time)
        if best_action is not None:
            return {"chargeTime": best_action}, 200
        else:
            return {"error": "no data for time slot"}, 404

    return app


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run()
