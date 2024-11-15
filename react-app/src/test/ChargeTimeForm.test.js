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
	} duration={"60"}/>);

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "15" } });
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "23" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "45" } });
	fireEvent.click(screen.getByText(/Calculate/i));
	
    expect(start).toBe('20:15');
	expect(end).toBe('23:45');
	expect(duration).toBe('60');
});

test('can not change start time to after end time', () => {
    var duration = "30";
    var changeDuration =(x) => duration = x;
	render(<ChargeTimeForm duration={duration} setDuration={changeDuration}/>);
    const startTimeHours = screen.getAllByRole("combobox")[0];
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "30" } });

    fireEvent.change(startTimeHours, { target: { value: "21" } });
	
    expect(startTimeHours.value).toBe('00');
});

test('can not change start time to more than duration before end time', () => {
    var duration = "45";
    var changeDuration =(x) => duration = x;
	render(<ChargeTimeForm duration={duration} setDuration={changeDuration}/>);
    const startTimeMinutes = screen.getAllByRole("combobox")[1];
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "45" } });
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });

    fireEvent.change(startTimeMinutes, { target: { value: "15" } });
	
    expect(startTimeMinutes.value).toBe('00');
});

test('can not change end time to before start time', () => {
    var duration = "30";
    var changeDuration =(x) => duration = x;
	render(<ChargeTimeForm duration={duration} setDuration={changeDuration}/>);
    const endTimeHours = screen.getAllByRole("combobox")[2];
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "15" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "45" } });

    fireEvent.change(endTimeHours, { target: { value: "15" } });
	
    expect(endTimeHours.value).toBe('24');
});

test('can not change end time to more than duration after start time', () => {
    var duration = "45";
    var changeDuration = (x) => duration = x;
	render(<ChargeTimeForm duration={duration} setDuration={changeDuration}/>);
    const endTimeHours = screen.getAllByRole("combobox")[2];
    const endTimeMinutes = screen.getAllByRole("combobox")[3];
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "30" } });
    fireEvent.change(endTimeHours, { target: { value: "23" } });
    fireEvent.change(endTimeMinutes, { target: { value: "45" } });
    fireEvent.change(endTimeHours, { target: { value: "17" } });
    
    fireEvent.change(endTimeMinutes, { target: { value: "00" } });
	
    expect(endTimeMinutes.value).toBe('45');
});

test('can not change duration if larger than gap between start and end', () => {
    var duration = "30";
    var changeDuration =(x) => duration = x;
	render(<ChargeTimeForm duration={duration} setDuration={changeDuration}/>);
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "30" } });
    fireEvent.change(screen.getAllByRole("combobox")[2], { target: { value: "17" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "15" } });

	fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '75'} });
	
    expect(screen.getByLabelText(/Duration/i).value).toBe('30');
});
