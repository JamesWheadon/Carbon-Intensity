import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import ChargeTimeForm from '../ChargeTimeForm';

test('renders start time input field and value can update', () => {
	render(<ChargeTimeForm />);

	const startTimeInput = screen.getByLabelText(/Start time:/i);
	fireEvent.change(startTimeInput, { target: { value: '20:24' } });

	expect(startTimeInput).toHaveValue('20:24');
});

test('renders end time input field and value can update', () => {
	render(<ChargeTimeForm />);

	const endTimeInput = screen.getByLabelText(/End time:/i);
	fireEvent.change(endTimeInput, { target: { value: '23:12' } });

	expect(endTimeInput).toHaveValue('23:12');
});

test('renders duration dropdown input field with default value', () => {
	render(<ChargeTimeForm />);

	expect(screen.getByLabelText(/Duration:/i)).toHaveValue('30');
});

test('renders duration dropdown input field and value can update', () => {
	render(<ChargeTimeForm />);

	const durationInput = screen.getByLabelText(/Duration:/i);
	userEvent.selectOptions(durationInput, '60 minutes');

	expect(durationInput).toHaveValue('60');
});

test('renders the submit button', () => {
	render(<ChargeTimeForm />);

	expect(screen.getByText(/Send/i)).toBeInTheDocument();
});

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

	fireEvent.change(screen.getByLabelText(/Start time:/i), { target: { value: '20:24' } });
	fireEvent.change(screen.getByLabelText(/End time:/i), { target: { value: '23:12' } });
	userEvent.selectOptions(screen.getByLabelText(/Duration:/i), '60 minutes');
	fireEvent.click(screen.getByText(/Send/i));
	
    expect(start).toBe('20:24');
	expect(end).toBe('23:12');
	expect(duration).toBe('60');
});
