import axios from 'axios';
import { getIntensityData } from '../getIntensityData';

jest.mock('axios');

test('retrieves intensity data', async () => {
	axios.post.mockImplementation(() => Promise.resolve({ data: { "date":"2024-11-11T00:00:00", 'intensities': [212,150,175] } }));

	const result = await getIntensityData();

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/intensities');
	expect(result).toStrictEqual([{"time": 1731283200000, "intensity": 212},{"time": 1731285000000, "intensity": 150},{"time": 1731286800000, "intensity": 175}]);
});
