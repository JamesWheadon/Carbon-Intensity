import axios from 'axios';
import { chargeTime, getChargeTime, createChargeTimeBody } from '../getChargeTime';

jest.mock('axios');

test('retrieves charge time for input', async () => {
	const body = {
		'startTime': '2024-10-28T20:12:00',
		'endTime': '2024-10-28T23:34:00',
		'duration': 60
	};
	axios.post.mockImplementation(() => Promise.resolve({ data: { 'chargeTime': '2024-09-30T21:00:00' } }));

	const result = await getChargeTime(body);

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/charge-time', body);
	expect(result).toStrictEqual({chargeTime: new Date("2024-09-30T20:00:00.000Z")});
});

test('creates charge time body', () => {
	const body = {
		'startTime': '2024-10-28T20:12:00',
		'endTime': '2024-10-28T23:34:00',
		'duration': 60
	};
	const date = new Date('2024-10-28');

	if (date.getTimezoneOffset() == 0) {
		const result = createChargeTimeBody(new Date('2024-10-28:20:12'), new Date('2024-10-28T23:34'), '60');

		expect(result).toStrictEqual(body);
	}
});

test('creates charge time body in BST', () => {
	const body = {
		'startTime': '2024-10-24T19:12:00',
		'endTime': '2024-10-24T22:34:00',
		'duration': 60
	};
	const date = new Date('2024-10-24');
    
	if (date.getTimezoneOffset() == -60) {
		const result = createChargeTimeBody(new Date('2024-10-24:20:12'), new Date('2024-10-24T23:34'), '60');

		expect(result).toStrictEqual(body);
	}
});

test('creates body from form output and gets charge time', async () => {
	axios.post.mockImplementation(() => Promise.resolve({ data: { 'chargeTime': '2024-09-30T21:00:00' } }));

	const result = await chargeTime(new Date('2024-10-30:20:12'), new Date('2024-10-30T23:34'), '60');

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/charge-time', expect.objectContaining({ 'duration': 60 }));
	expect(result).toStrictEqual({chargeTime: new Date("2024-09-30T20:00:00.000Z")});
});

test('catches request error and returns error body', async () => {
	axios.post.mockRejectedValueOnce();

	const result = await chargeTime(new Date('2024-10-30:20:12'), new Date('2024-10-30T23:34'), '60');

	expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/charge-time', expect.objectContaining({ 'duration': 60 }));
	expect(result).toStrictEqual({error: "unable to get best charge time"});
});
