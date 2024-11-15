import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import axios from "axios";
import ChargeTime from '../ChargeTime';

jest.mock("axios");

const intensityData = Array(48).fill({time: new Date('2024-10-24'), intensity: 100});

test('gets best charge time', () => {
    axios.post.mockImplementation(() => Promise.resolve({ data: { 'chargeTime': '2024-09-30T21:00:00' } }));
    render(<ChargeTime intensityData={intensityData}/>);

    fireEvent.change(screen.getByLabelText(/Start time/i), { target: { value: '20:24' } });
    fireEvent.change(screen.getByLabelText(/End time/i), { target: { value: '23:12' } });
    userEvent.selectOptions(screen.getByLabelText(/Duration/i), '60 minutes');
    fireEvent.click(screen.getByText(/Calculate/i));
    
    waitFor(() => expect(screen.getByText(/Best Time: 21:00/i)).toBeInTheDocument());
});

test('displays error when can not get best charge time', () => {
	axios.post.mockRejectedValueOnce();
    render(<ChargeTime intensityData={intensityData}/>);

    fireEvent.change(screen.getByLabelText(/Start time/i), { target: { value: '20:24' } });
    fireEvent.change(screen.getByLabelText(/End time/i), { target: { value: '23:12' } });
    userEvent.selectOptions(screen.getByLabelText(/Duration/i), '60 minutes');
    fireEvent.click(screen.getByText(/Calculate/i));
    
    waitFor(() => expect(screen.getByText(/Could not get best charge time, please try again/i)).toBeInTheDocument());
});
