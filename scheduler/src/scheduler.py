import random
import datetime

import numpy as np


class Scheduler:
    def __init__(self):
        self.intensities_date = None

    def action_index_from_timestamp(self, timestamp):
        minutes_diff = (timestamp - self.intensities_date).total_seconds() // 60.0
        return minutes_diff // 30


class UseTimeScheduler(Scheduler):
    def __init__(self):
        super().__init__()
        self.alpha = 0.1
        self.gamma = 0.2
        self.epsilon = 1.0
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.num_episodes = 1000
        self.num_time_slots = 48
        self.Q_table = np.zeros((self.num_time_slots, self.num_time_slots))

    def calculate_schedules(self, intensities, intensities_date):
        self.intensities_date = intensities_date
        env = CarbonIntensityEnv(intensities)
        state = 0
        for episode in range(self.num_episodes):
            while state != self.num_time_slots:
                if random.uniform(0, 1) < self.epsilon:
                    action = random.randint(state, self.num_time_slots - 1)
                else:
                    action = np.argmax(self.Q_table[state][state:]) + state

                reward = env.step(action)
                best_next_action = np.argmax(self.Q_table[action])
                self.Q_table[state, action] = (self.Q_table[state, action] + self.alpha *
                                               (reward + self.gamma * self.Q_table[action, best_next_action] -
                                                self.Q_table[state, action]))
                state += 1

            if self.epsilon > self.epsilon_min:
                self.epsilon *= self.epsilon_decay
            state = 0

    def best_action_for(self, timestamp, end_timestamp=None):
        check_action_timestamps(end_timestamp, timestamp)
        if self.intensities_date is None or timestamp < self.intensities_date:
            return None
        action_index = self.action_index_from_timestamp(timestamp)
        end_action_index = min(self.action_index_from_timestamp(end_timestamp), 48) if end_timestamp is not None else 47
        try:
            action_to_take = np.argmax(self.Q_table[action_index][action_index:end_action_index]) + action_index
            return self.intensities_date + datetime.timedelta(seconds = int(action_to_take) * 1800)
        except IndexError:
            return None

    def print_q_table(self):
        np.set_printoptions(threshold=self.num_time_slots * self.num_time_slots)
        print(self.Q_table)


def check_action_timestamps(end_timestamp, timestamp):
    if end_timestamp is not None and end_timestamp < timestamp:
        raise ValueError("End must be after current")


class CarbonIntensityEnv:
    def __init__(self, day_intensities):
        self.day_intensities = day_intensities

    def step(self, action):
        intensity = self.day_intensities[action]

        reward = -intensity
        return reward
