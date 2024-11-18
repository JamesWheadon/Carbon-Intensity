import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ChargeTimeForm from '../ChargeTimeForm';

test('form data is passed to getChargeTime when form submitted', () => {
    const startDay = new Date();
    startDay.setUTCHours(0,0,0,0);
    var start = '';
    var end = '';
    var duration = '';
    render(<ChargeTimeForm getChargeTime={
        (a, b, c) => {
            start = a;
            end = b;
            duration = c;
        }
    } duration={"60"} />);

    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "23" } });
    fireEvent.change(screen.getAllByRole("combobox")[4], { target: { value: "3" } });
    fireEvent.click(screen.getByText(/Calculate/i));

    expect(start).toStrictEqual(new Date(startDay.getTime() + 81 * 15 * 60 * 1000));
    expect(end).toStrictEqual(new Date(startDay.getTime() + 191 * 15 * 60 * 1000));
    expect(duration).toBe('60');
});

test('can not change start time to after end time', () => {
    var duration = "30";
    var changeDuration = (x) => duration = x;
    render(<ChargeTimeForm duration={duration} setDuration={changeDuration} />);
    const startTimeHours = screen.getAllByRole("combobox")[0];
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "0" } });

    expect(startTimeHours.value).toBe("0");
});

test('can not change start time to more than duration before end time', () => {
    var duration = "45";
    var changeDuration = (x) => duration = x;
    render(<ChargeTimeForm duration={duration} setDuration={changeDuration} />);
    const startTimeMinutes = screen.getAllByRole("combobox")[1];
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[4], { target: { value: "3" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "0" } });
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "20" } });

    fireEvent.change(startTimeMinutes, { target: { value: "1" } });

    expect(startTimeMinutes.value).toBe("0");
});

test('can not change end time to before start time', () => {
    var duration = "30";
    var changeDuration = (x) => duration = x;
    render(<ChargeTimeForm duration={duration} setDuration={changeDuration} />);
    const endTimeHours = screen.getAllByRole("combobox")[3];
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(endTimeHours, { target: { value: "20" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "0" } });
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "15" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "3" } });

    fireEvent.change(endTimeHours, { target: { value: "15" } });

    expect(endTimeHours.value).toBe("20");
});

test('can not change end time to more than duration after start time', () => {
    var duration = "45";
    var changeDuration = (x) => duration = x;
    render(<ChargeTimeForm duration={duration} setDuration={changeDuration} />);
    const endTimeHours = screen.getAllByRole("combobox")[3];
    const endTimeMinutes = screen.getAllByRole("combobox")[4];
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "2" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(endTimeMinutes, { target: { value: "3" } });
    fireEvent.change(endTimeHours, { target: { value: "23" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "0" } });
    fireEvent.change(endTimeHours, { target: { value: "17" } });

    fireEvent.change(endTimeMinutes, { target: { value: "0" } });

    expect(endTimeMinutes.value).toBe("3");
});

test('can not change duration if larger than gap between start and end', () => {
    var duration = "30";
    var changeDuration = (x) => duration = x;
    render(<ChargeTimeForm duration={duration} setDuration={changeDuration} />);
    fireEvent.change(screen.getAllByRole("combobox")[0], { target: { value: "16" } });
    fireEvent.change(screen.getAllByRole("combobox")[1], { target: { value: "2" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[3], { target: { value: "17" } });
    fireEvent.change(screen.getAllByRole("combobox")[4], { target: { value: "1" } });
    fireEvent.change(screen.getAllByRole("combobox")[5], { target: { value: "0" } });

    fireEvent.change(screen.getByLabelText(/Duration/i), { target: { value: '75' } });

    expect(screen.getByLabelText(/Duration/i).value).toBe("30");
    expect(duration).toBe("30");
});
