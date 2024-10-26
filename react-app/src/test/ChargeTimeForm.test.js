import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeForm from '../ChargeTimeForm';

test('renders learn react link', () => {
  render(<ChargeTimeForm />);
  const startTimeLabel = screen.getByText(/Start time:/i);
  expect(startTimeLabel).toBeInTheDocument();
});
