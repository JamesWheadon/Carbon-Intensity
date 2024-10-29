import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from '../App';

test('renders learn react link', () => {
  render(<App />);

  const linkElement = screen.getByText(/learn react/i);

  expect(linkElement).toBeInTheDocument();
});

test('renders website title', () => {
  render(<App />);

  const websiteTitle = screen.getByText(/When to Wash/i);

  expect(websiteTitle).toBeInTheDocument();
})

test('no best charge time displayed when no data', async () => {
  render(<App />);

  expect(screen.getByText(/Start time:/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/End time:/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/Duration:/i)).toBeInTheDocument();
  expect(screen.queryByText(/Best Time:/i)).toBeNull();
})
