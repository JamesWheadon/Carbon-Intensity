import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from '../App';

test('renders learn react link', () => {
    render(<App />);

    expect(screen.getByText(/learn react/i)).toBeInTheDocument();
});

test('renders website title', () => {
    render(<App />);

    expect(screen.getByText(/When to Wash/i)).toBeInTheDocument();
});

test('no best charge time displayed when no data', async () => {
    render(<App />);

    expect(screen.getByText(/Start time/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/End time/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Duration/i)).toBeInTheDocument();
    expect(screen.queryByText(/Best Time/i)).toBeNull();
});
