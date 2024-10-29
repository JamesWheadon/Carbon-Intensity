import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import axios from "axios";
import '@testing-library/jest-dom';
import App from '../App';

jest.mock("axios");

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

test('gets best charge time', async () => {
  axios.post.mockImplementation(() => Promise.resolve({ data: { "chargeTime": "2024-09-30T21:00:00" } }));
  render(<App />);

  fireEvent.change(screen.getByLabelText(/Start time:/i), { target: { value: '20:24' } });
  fireEvent.change(screen.getByLabelText(/End time:/i), { target: { value: '23:12' } });
  userEvent.selectOptions(screen.getByLabelText(/Duration:/i), "60 minutes");
  fireEvent.click(screen.getByText(/Send/i));
  
  await waitFor(() => expect(screen.getByText(/Best Time: 21:00/i)).toBeInTheDocument());
})

test('no best charge time displayed when no data', async () => {
  render(<App />);
  expect(screen.queryByText(/Best Time:/i)).toBeNull();
})
