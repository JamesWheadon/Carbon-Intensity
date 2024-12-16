import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeMessage, { dateTimeToDisplayTime, getCarbonSaving } from "../carbonSaving";

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

test('displays best charge time information', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 50};
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 50};
    render(<ChargeTimeMessage chargeTime={new Date('2024-10-24T03:00:00')} intensityData={intensityData} duration={60} comparisonTime={new Date('2024-10-24')}/>);
    
    expect(screen.getByText(/Best Time: 03:00 24\/10/i)).toBeInTheDocument();
    expect(screen.getByText(/Saving: 50 gCO2\/kWh/i)).toBeInTheDocument();
});

test('displays different message with negative intensity', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 150};
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 150};
    render(<ChargeTimeMessage chargeTime={new Date('2024-10-24T03:00:00')} intensityData={intensityData} duration={60} comparisonTime={new Date('2024-10-24')}/>);
    
    expect(screen.getByText(/Best Time: 03:00 24\/10/i)).toBeInTheDocument();
    expect(screen.getByText(/Extra Intensity: 50 gCO2\/kWh/i)).toBeInTheDocument();
});
