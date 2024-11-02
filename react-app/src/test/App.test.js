import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from '../App';

test('no best charge time displayed when no data', async () => {
    render(<App />);

    expect(screen.getByText(/Start time/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/End time/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Duration/i)).toBeInTheDocument();
    expect(screen.queryByText(/Best Time/i)).toBeNull();
});
