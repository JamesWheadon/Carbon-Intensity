import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTime from '../ChargeTime';

test('renders best charge time text', () => {
    render(<ChargeTime bestChargeTime={"13:30"}/>);
    const bestTime = screen.getByText(/Best Time: 13:30/i);
    expect(bestTime).toBeInTheDocument();
});
