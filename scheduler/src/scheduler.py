import random
import datetime

import numpy as np


class Scheduler:
    def __init__(self):
        self.intensities_date = None
        self.env = None

    def action_index_from_timestamp(self, timestamp):
        minutes_diff = (timestamp - self.intensities_date).total_seconds() // 60.0
        return int(minutes_diff // 15)

    def get_intensities(self):
        if self.env is None:
            return None
        return {
            "intensities": self.env.get_intensities(),
            "date": self.intensities_date
        }


class UseTimeScheduler(Scheduler):
    def __init__(self):
        super().__init__()
        self.alpha = 0.1
        self.gamma = 0.2
        self.epsilon = 1.0
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.num_episodes = 1000
        self.num_time_slots = 96
        self.durations = [2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20]
        self.Q_table = np.zeros((len(self.durations), self.num_time_slots, self.num_time_slots))

    def calculate_schedules(self, intensities, intensities_date):
        self.intensities_date = intensities_date
        self.env = CarbonIntensityEnv(intensities)
        state = 0
        for episode in range(self.num_episodes):
            for duration in self.durations:
            # duration = self.durations[random.randint(0, len(self.durations) - 1)]
                duration_index = self.durations.index(duration)
                while state + duration < self.num_time_slots:
                    if random.uniform(0, 1) < self.epsilon:
                        action = random.randint(state, self.num_time_slots - duration)
                    else:
                        action = np.argmax(self.Q_table[duration_index][state][state:self.num_time_slots - duration + 1]) + state

                    reward = self.env.step(action, duration)
                    best_next_action = np.argmax(self.Q_table[duration_index][action])
                    self.Q_table[duration_index, state, action] = (self.Q_table[duration_index, state, action] + self.alpha *
                                                   (reward + self.gamma * self.Q_table[duration_index, action, best_next_action] -
                                                    self.Q_table[duration_index, state, action]))
                    state += 1
                state = 0

            if self.epsilon > self.epsilon_min:
                self.epsilon *= self.epsilon_decay

    def best_action_for(self, timestamp, duration, end_timestamp=None):
        check_action_timestamps(end_timestamp, timestamp)
        if self.intensities_date is None or timestamp < self.intensities_date:
            return None
        action_index = self.action_index_from_timestamp(timestamp)
        end_action_index = min(self.action_index_from_timestamp(end_timestamp) + 1 - duration, 97 - duration) if end_timestamp is not None else 97 - duration
        try:
            action_to_take = np.argmax(self.Q_table[self.durations.index(duration)][action_index][action_index:end_action_index]) + action_index
            return self.intensities_date + datetime.timedelta(seconds = int(action_to_take) * 900)
        except IndexError:
            return None

    def print_q_table(self):
        np.set_printoptions(edgeitems=30, linewidth=100000, threshold=self.num_time_slots * self.num_time_slots)
        for duration in self.durations:
            print(f"{duration * 15} minutes")
            print(self.Q_table[self.durations.index(duration)])


def check_action_timestamps(end_timestamp, timestamp):
    if end_timestamp is not None and end_timestamp < timestamp:
        raise ValueError("End must be after current")


class CarbonIntensityEnv:
    def __init__(self, day_intensities):
        self.day_intensities = np.repeat(day_intensities, 2)

    def step(self, action, duration):
        steps = duration
        reward = 0
        for s in range(steps):
            reward -= self.day_intensities[action + s]
        return reward

    def get_intensities(self):
        return self.day_intensities[::2].tolist()


if __name__ == "__main__":
    scheduler = UseTimeScheduler()
    scheduler.calculate_schedules([134, 175, 243, 117, 208, 125, 87, 202, 58, 145, 134, 222, 133, 236, 140, 222, 87, 207, 199, 125, 100, 218, 236, 154, 215, 157, 151, 105, 107, 240, 53, 230, 249, 192, 70, 97, 99, 62, 116, 181, 144, 229, 127, 173, 69, 122, 146, 75], datetime.datetime.fromisoformat('2024-10-15T01:00:00'))
    scheduler.print_q_table()
    print(scheduler.best_action_for(datetime.datetime.fromisoformat('2024-10-15T14:00:00'), duration = 3))
    print(scheduler.best_action_for(datetime.datetime.fromisoformat('2024-10-15T14:00:00'), duration = 6))
