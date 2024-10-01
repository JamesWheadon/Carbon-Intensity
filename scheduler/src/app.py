from datetime import datetime

from flask import Flask, request
from flask_expects_json import expects_json
from jsonschema.exceptions import ValidationError

from src.scheduler import UseTimeScheduler

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
            "pattern": '^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$'
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
        try:
            best_action = app.config["SCHEDULER"].best_action_for(current_time, end_time)
            if best_action is not None:
                return {"chargeTime": best_action.isoformat()}, 200
            else:
                return {"error": "No data for time slot"}, 404
        except ValueError as e:
            return {"error": str(e)}, 400

    @app.route("/intensities", methods=['POST'])
    @expects_json(intensities_schema)
    def intensities():
        carbon_intensities = request.json["intensities"]
        data_date = to_datetime(request.json["date"])
        app.config["SCHEDULER"].calculate_schedules(carbon_intensities, data_date)
        return '', 204

    @app.errorhandler(400)
    def bad_request(error):
        if isinstance(error.description, ValidationError):
            original_error = error.description
            return {'error': original_error.message}, 400
        return error

    return app


def to_datetime(datetime_string):
    return datetime.strptime(datetime_string, "%Y-%m-%dT%H:%M:%S")


if __name__ == "__main__":
    create_app(UseTimeScheduler()).run(port=8000)
