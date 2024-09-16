class CarbonIntensityEnv:
    def reset(self):
        return 0

    def step(self, action):
        carbon_intensities = [227, 239, 270, 113, 125, 190, 202, 131, 178, 207, 172, 163, 151, 109, 297, 107, 126, 159, 170,
                        103, 143, 190, 235, 200, 244, 137, 241, 106, 265, 103, 114, 262, 156, 261, 165, 210, 183, 118,
                        164, 241, 235, 119, 121, 160, 119, 185, 134, 263]
        intensity = carbon_intensities[action]

        reward = -intensity
        next_state = action
        return next_state, reward
