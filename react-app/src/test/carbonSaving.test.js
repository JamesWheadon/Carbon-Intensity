import { dateTimeToDisplayTime, getCarbonSaving } from "../carbonSaving";

const intensityData = Array(96).fill({time: new Date('2024-10-24'), intensity: 100});

test('converts date to properly formatted timestamp', () => {
	const result = dateTimeToDisplayTime(new Date(1731600000));

	expect(result).toStrictEqual("02:00 21/01");
});

test('gets carbon savings', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 50};
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 50};
    const result = getCarbonSaving(new Date('2024-10-24T03:00:00'), intensityData, 60, new Date('2024-10-24'));

    expect(result).toStrictEqual(50);
});

test('gets carbon savings for the next day', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 50};
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 50};
    const result = getCarbonSaving(new Date('2024-10-25T03:00:00'), intensityData, 60, new Date('2024-10-24'));

    expect(result).toStrictEqual(0);
});
