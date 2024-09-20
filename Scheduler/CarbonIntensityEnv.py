class CarbonIntensityEnv:
    def __init__(self, day_intensities):
        self.day_intensities = day_intensities

    def step(self, action):
        intensity = self.day_intensities[action]

        reward = -intensity
        return reward
