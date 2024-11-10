import axios from 'axios';
import { getIntensityData } from '../getIntensityData';

jest.mock('axios');

test('retrieves intensity data', async () => {
	axios.post.mockImplementation(() => Promise.resolve({ data: { 'intensities': [212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175] } }));

	const result = await getIntensityData();

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/intensities');
	expect(result).toStrictEqual({'intensities': [212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175,212,150,175]});
});
