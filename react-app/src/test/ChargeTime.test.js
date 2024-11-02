import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import axios from "axios";
import ChargeTime from '../ChargeTime';

jest.mock("axios");

test('gets best charge time', async () => {
    axios.post.mockImplementation(() => Promise.resolve({ data: { 'chargeTime': '2024-09-30T21:00:00' } }));
    render(<ChargeTime />);

    fireEvent.change(screen.getByLabelText(/Start time/i), { target: { value: '20:24' } });
    fireEvent.change(screen.getByLabelText(/End time/i), { target: { value: '23:12' } });
    userEvent.selectOptions(screen.getByLabelText(/Duration/i), '60 minutes');
    fireEvent.click(screen.getByText(/Calculate/i));
    
    await waitFor(() => expect(screen.getByText(/Best Time: 21:00/i)).toBeInTheDocument());
});
