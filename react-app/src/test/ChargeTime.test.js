import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTime from '../ChargeTime';

test('renders start time label', () => {
    render(<ChargeTime />);
    const bestTime = screen.getByText(/Best Time:/i);
    expect(bestTime).toBeInTheDocument();
});