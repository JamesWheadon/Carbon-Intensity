import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeMessage from "../carbonSaving";

const intensityData = Array(96).fill({time: new Date('2024-10-24'), intensity: 100});

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

test('displays no intensity when it is equal to current', () => {
    intensityData[4] = {time: new Date('2024-10-24'), intensity: 100};
    intensityData[5] = {time: new Date('2024-10-24'), intensity: 100};
    render(<ChargeTimeMessage chargeTime={new Date('2024-10-24T03:00:00')} intensityData={intensityData} duration={60} comparisonTime={new Date('2024-10-24')}/>);
    
    expect(screen.getByText(/Best Time: 03:00 24\/10/i)).toBeInTheDocument();
    expect(screen.queryByText(/gCO2\/kWh/i)).toBeNull();
});
