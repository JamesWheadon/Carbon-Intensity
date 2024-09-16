import numpy as np
import random

from CarbonIntensityEnv import CarbonIntensityEnv

alpha = 0.1
gamma = 0.2
epsilon = 1.0
epsilon_min = 0.01
epsilon_decay = 0.995

num_episodes = 1000
num_time_slots = 48

Q_table = np.zeros((num_time_slots, num_time_slots))
env = CarbonIntensityEnv()

state = env.reset()
for episode in range(num_episodes):
    while True:
        if random.uniform(0, 1) < epsilon:
            action = random.randint(state, num_time_slots - 1)
        else:
            action = np.argmax(Q_table[state][state:]) + state

        next_state, reward = env.step(action)

        best_next_action = np.argmax(Q_table[next_state])
        Q_table[state, action] = (Q_table[state, action] + alpha *
                                  (reward + gamma * Q_table[next_state, best_next_action] - Q_table[state, action]))
        if state == num_time_slots - 1:
            break
        state = next_state

    if epsilon > epsilon_min:
        epsilon *= epsilon_decay
    state = env.reset()

np.set_printoptions(threshold=num_time_slots * num_time_slots)
print("Final Q-table:")
print(Q_table)
