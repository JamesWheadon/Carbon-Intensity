import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeForm from '../ChargeTimeForm';

test('renders start time label', () => {
  render(<ChargeTimeForm />);
  const startTimeLabel = screen.getByText(/Start time:/i);
  expect(startTimeLabel).toBeInTheDocument();
});

test('renders start time input field and value can update', () => {
  render(<ChargeTimeForm />);
  const startTimeInput = screen.getByLabelText(/Start time:/i);
  fireEvent.change(startTimeInput, {target: {value: '20:24'}});
  expect(startTimeInput).toHaveValue("20:24");
});

test('renders end time input field and value can update', () => {
  render(<ChargeTimeForm />);
  const endTimeInput = screen.getByLabelText(/End time:/i);
  fireEvent.change(endTimeInput, {target: {value: '23:12'}});
  expect(endTimeInput).toHaveValue("23:12");
});

test('renders duration dropdown input field and value can update', () => {
  render(<ChargeTimeForm />);
  const durationInput = screen.getByLabelText(/Duration:/i);
  expect(durationInput).toHaveValue("30");
});
