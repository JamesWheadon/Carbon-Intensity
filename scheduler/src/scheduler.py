import numpy as np
import random

# from environments import CarbonIntensityEnv


class UseTimeScheduler:
    def __init__(self):
        self.alpha = 0.1
        self.gamma = 0.2
        self.epsilon = 1.0
        self.epsilon_min = 0.01
        self.epsilon_decay = 0.995
        self.num_episodes = 1000
        self.num_time_slots = 48
        self.Q_table = np.zeros((self.num_time_slots, self.num_time_slots))

    def calculate_schedules(self, intensities):
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

    def get_best_action_for_time_slot(self, time_slot):
        return np.argmax(self.Q_table[time_slot][time_slot:]) + time_slot

    def print_q_table(self):
        np.set_printoptions(threshold=self.num_time_slots * self.num_time_slots)
        print(self.Q_table)


class CarbonIntensityEnv:
    def __init__(self, day_intensities):
        self.day_intensities = day_intensities

    def step(self, action):
        intensity = self.day_intensities[action]

        reward = -intensity
        return reward


if __name__ == "__main__":
    scheduler = UseTimeScheduler()
    scheduler.calculate_schedules(
        [227, 239, 270, 113, 125, 190, 202, 131, 178, 207, 172, 163, 151, 109, 297, 107, 126, 159, 170, 103, 143, 190,
         235, 200, 244, 137, 241, 106, 265, 103, 114, 262, 156, 261, 165, 210, 183, 118, 164, 241, 235, 119, 121, 160,
         119, 185, 134, 263]
    )
    print(scheduler.get_best_action_for_time_slot(20))
