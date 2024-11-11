import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import IntensityDataPoint from '../IntensityDataPoint';

test('displays data point', () => {
	render(<IntensityDataPoint dataPoint={{"time": 1731364200000, "intensity": 100}}/>);

    expect(screen.getByText(/22:30/i)).toBeInTheDocument()
    expect(screen.getByText(/100/i)).toBeInTheDocument()
    expect(screen.getByText(/clear/i)).toBeInTheDocument()
});

test('clear button calls clear function', () => {
    var clicked = true;
    function clear() {
        clicked = false
    }
	render(<IntensityDataPoint dataPoint={{"time": 1731364200000, "intensity": 100}} clear={clear}/>);

    fireEvent.click(screen.getByText(/clear/i))
    expect(clicked).toBe(false)
});