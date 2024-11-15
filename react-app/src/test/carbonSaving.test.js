import { dateTimeToDisplayTime, getCarbonSaving } from "../carbonSaving";

const intensityData = Array(48).fill({time: new Date('2024-10-24'), intensity: 100});

test('converts date to properly formatted timestamp', () => {
	const result = dateTimeToDisplayTime(new Date(1731600000));

	expect(result).toStrictEqual("02:00");
});

test('gets carbon savings', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 50}
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 50}
    const result = getCarbonSaving(new Date(1731679030), intensityData, 60);

    expect(result).toStrictEqual(50);
});
