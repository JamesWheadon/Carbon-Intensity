from datetime import datetime

from flask import Flask, request
from flask_expects_json import expects_json
from jsonschema.exceptions import ValidationError

from src.scheduler import UseTimeScheduler, InvalidChargeTimeError, UntrainedDurationError

intensities_schema = {
    'type': 'object',
    'properties': {
        'intensities': {
            'type': 'array',
            "items": {
                "type": "integer"
            },
            "minItems": 48,
            "maxItems": 48
        },
        'date': {
            'type': 'string',
            "pattern": '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}$'
        }
    },
    'required': ['intensities', 'date']
}


def create_app(scheduler):
    app = Flask(__name__)
    app.config['SCHEDULER'] = scheduler

    @app.route("/charge-time")
    def charge_time():
        current_time = request.args.get("current", type=to_datetime)
        end_time = request.args.get("end", type=to_datetime)
        duration = request.args.get("duration", type=int, default=30)
        charge_scheduler = app.config["SCHEDULER"]
        slot_span = min(charge_scheduler.durations, key=lambda x: abs(x - duration / 15))
        best_action = charge_scheduler.best_action_for(current_time, slot_span, end_timestamp=end_time)
        if best_action is not None:
            return {"chargeTime": best_action.isoformat()}, 200
        else:
            return {"error": "No data for time slot"}, 404

    @app.route("/intensities", methods=["POST"])
    @expects_json(intensities_schema)
    def set_intensities():
        carbon_intensities = request.json["intensities"]
        data_date = to_datetime(request.json["date"])
        app.config["SCHEDULER"].set_intensities(carbon_intensities, data_date)
        return '', 204

    @app.route("/intensities", methods=["GET"])
    def get_intensities():
        intensities = app.config["SCHEDULER"].get_intensities()
        if intensities:
            return {"intensities": intensities["intensities"], "date": from_datetime(intensities["date"])}, 200
        return {"error": "No intensity data for scheduler"}, 404

    @app.route("/intensities", methods=["DELETE"])
    def clear_data():
        app.config["SCHEDULER"].clear_data()
        return '', 204

    @app.route("/intensities/train", methods=["PATCH"])
    def train_for_duration():
        duration = request.args.get("duration", type=int)
        charge_scheduler = app.config["SCHEDULER"]
        slot_span = min(charge_scheduler.durations, key=lambda x: abs(x - duration / 15))
        charge_scheduler.train(slot_span)
        return '', 204

    @app.errorhandler(400)
    def bad_request(error):
        if isinstance(error.description, ValidationError):
            original_error = error.description
            return {'error': original_error.message}, 400
        return error

    @app.errorhandler(TypeError)
    def handle_type_error(error):
        return {'error': error.args[0]}, 400

    @app.errorhandler(ValueError)
    def handle_value_error(error):
        return {'error': str(error)}, 400

    @app.errorhandler(InvalidChargeTimeError)
    def handle_value_error(error):
        return {'error': str(error)}, 404

    @app.errorhandler(UntrainedDurationError)
    def handle_value_error(error):
        return {'error': str(error)}, 404

    return app


def to_datetime(datetime_string):
    return datetime.strptime(datetime_string, "%Y-%m-%dT%H:%M:%S")

def from_datetime(datetime_object):
    return datetime_object.isoformat()[:19]


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run(port=8000)
