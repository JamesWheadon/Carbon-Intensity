import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import TimeRange from '../TimeRange';

test('dragging start and end changes the values', () => {
    var start = '0';
    var end = '96';
    render(<TimeRange start={start} end={end} moveStart={(s) => { start = s }} moveEnd={(e) => { end = e }} />);

    fireEvent.change(screen.getAllByRole("slider")[0], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("slider")[1], { target: { value: "70" } });


    expect(start).toBe('20');
    expect(end).toBe('70');
});
