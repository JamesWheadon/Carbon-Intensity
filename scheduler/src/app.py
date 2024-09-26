from flask import Flask, request

from src.scheduler import UseTimeScheduler


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/charge-time")
    def charge_time():
        current_time = request.args.get("current", type=int)
        best_action = app.config["SCHEDULER"].best_action_for(current_time)
        if best_action is not None:
            return {"chargeTime": int(best_action)}, 200
        else:
            return {"error": "No data for time slot"}, 404

    @app.route("/intensities", methods=['POST'])
    def intensities():
        carbon_intensities = request.json["intensities"]
        date = request.json["date"]
        if len(carbon_intensities) == 48:
            app.config["SCHEDULER"].calculate_schedules(carbon_intensities, date)
            return '', 204
        else:
            return {"error": "Invalid intensities, should be an array of 48 time slots"}, 422

    @app.route("/intensities/date", methods=['GET'])
    def intensities_date():
        date_of_intensities = app.config["SCHEDULER"].day_of_data()
        if date_of_intensities is not None:
            return {"date": date_of_intensities.isoformat()}, 200
        else:
            return {"error": "No data has been submitted to the scheduler"}, 404

    return app

if __name__ == "__main__":
    create_app(UseTimeScheduler()).run(port=8000)
