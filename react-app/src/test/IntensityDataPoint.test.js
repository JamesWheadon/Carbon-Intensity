import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import IntensityDataPoint from '../IntensityDataPoint';

test('displays data point', () => {
	render(<IntensityDataPoint dataPoint={{"time": new Date("2024-11-11T22:30:00.000Z"), "intensity": 100}}/>);

    expect(screen.getByText(/22:30 to 23:00 11\/11/i)).toBeInTheDocument()
    expect(screen.getByText(/100/i)).toBeInTheDocument()
});
