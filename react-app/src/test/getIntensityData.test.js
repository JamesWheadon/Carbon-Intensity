import axios from 'axios';
import { getIntensityData } from '../getIntensityData';

jest.mock('axios');

test('retrieves intensity data', async () => {
	axios.post.mockImplementation(() => Promise.resolve({ data: { "date":"2024-11-11T00:00:00", 'intensities': Array(32).fill([212,150,175]).flat() } }));

	const result = await getIntensityData();

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/intensities');
	expect(result[0]).toStrictEqual({"time": new Date("2024-11-11T00:00:00.000Z"), "intensity": 212});
	expect(result[1]).toStrictEqual({"time": new Date("2024-11-11T00:30:00.000Z"), "intensity": 150});
	expect(result[2]).toStrictEqual({"time": new Date("2024-11-11T01:00:00.000Z"), "intensity": 175});
	expect(result[95]).toStrictEqual({"time": new Date("2024-11-12T23:30:00.000Z"), "intensity": 175});
});
