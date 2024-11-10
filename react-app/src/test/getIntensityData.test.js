import axios from 'axios';
import { getIntensityData } from '../getIntensityData';

jest.mock('axios');

test('retrieves intensity data', async () => {
	axios.post.mockImplementation(() => Promise.resolve({ data: { 'intensities': [212,150,175] } }));

	const result = await getIntensityData();

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/intensities');
	expect(result).toStrictEqual([{"time": "00:15", "intensity": 212},{"time": "00:45", "intensity": 150},{"time": "01:15", "intensity": 175}]);
});
