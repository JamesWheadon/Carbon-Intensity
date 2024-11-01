import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeForm from '../ChargeTimeForm';

test('form data is passed to getChargeTime when form submitted', () => {
	var start = '';
	var end = '';
	var duration = '';
	render(<ChargeTimeForm getChargeTime={
		(a, b, c) => {
			start = a;
			end = b;
			duration = c;
		}
	} />);

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "15" } });
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "23" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "45" } });
	fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '60'} });
	fireEvent.click(screen.getByText(/Send/i));
	
    expect(start).toBe('20:15');
	expect(end).toBe('23:45');
	expect(duration).toBe('60');
});

test('can not change start time to after end time', () => {
	render(<ChargeTimeForm />);
    const startTimeHours = screen.getAllByRole("combobox")[0];
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "30" } });

    fireEvent.change(startTimeHours, { target: { value: "21" } });
	
    expect(startTimeHours.value).toBe('00');
});

test('can not change start time to more than duration before end time', () => {
	render(<ChargeTimeForm />);
    const startTimeMinutes = screen.getAllByRole("combobox")[1];
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "45" } });
	fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '45'} });
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });

    fireEvent.change(startTimeMinutes, { target: { value: "15" } });
	
    expect(startTimeMinutes.value).toBe('00');

	fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '30'} });

    fireEvent.change(startTimeMinutes, { target: { value: "15" } });
	
    expect(startTimeMinutes.value).toBe('15');
});

test('can not change end time to before start time', () => {
	render(<ChargeTimeForm />);
    const endTimeHours = screen.getAllByRole("combobox")[2];
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "15" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "45" } });

    fireEvent.change(endTimeHours, { target: { value: "15" } });
	
    expect(endTimeHours.value).toBe('24');
});

test('can not change end time to more than duration after start time', () => {
	render(<ChargeTimeForm />);
    const endTimeHours = screen.getAllByRole("combobox")[2];
    const endTimeMinutes = screen.getAllByRole("combobox")[3];
    const duration = screen.getByLabelText(/Duration/i);
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "30" } });
	fireEvent.change(duration, { target: { value: '45'} });
    fireEvent.change(endTimeHours, { target: { value: "23" } });
    fireEvent.change(endTimeMinutes, { target: { value: "45" } });
    fireEvent.change(endTimeHours, { target: { value: "17" } });
    
    fireEvent.change(endTimeMinutes, { target: { value: "00" } });
	
    expect(endTimeMinutes.value).toBe('45');

	fireEvent.change(duration, { target: { value: '30'} });

    fireEvent.change(endTimeMinutes, { target: { value: "00" } });
	
    expect(endTimeMinutes.value).toBe('00');
});

test('can not change change duration if larger than gap between start and end', () => {
	render(<ChargeTimeForm />);
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "30" } });
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "17" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "15" } });

	fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '75'} });
	
    expect(screen.getByLabelText(/Duration/i).value).toBe('30');
});
