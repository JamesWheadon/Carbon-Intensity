import os

from src.app import create_app
from src.scheduler import UseTimeScheduler

app = create_app(UseTimeScheduler())

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 8080)))
