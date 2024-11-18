import { useState } from 'react';
import TimeRange from './TimeRange';
import './ChargeTimeForm.css';

function ChargeTimeForm({ getChargeTime, duration, setDuration }) {
    const [start, setStart] = useState(0);
    const [end, setEnd] = useState(192);
    const submit = (e) => {
        e.preventDefault();
        getChargeTime(getDate(start), getDate(end), duration);
    };
    return (
        <form onSubmit={submit}>
            <TimeRange start={start} end={end} moveStart={(t) => moveStartSlider(t, end, duration, setStart)} moveEnd={(t) => moveEndSlider(t, start, duration, setEnd)} />
            <div id="pickers">
                <TimePicker labelText={"Start Time"} time={start} setTime={(t) => moveStartSlider(t, end, duration, setStart)} />
                <TimePicker labelText={"End Time"} time={end} setTime={(t) => moveEndSlider(t, start, duration, setEnd)} />
            </div>
            <label>Duration
                <select
                    value={duration}
                    onChange={(i) => changeDuration(i.target.value, start, end, setDuration)}
                >
                    <option value="30">30 minutes</option>
                    <option value="45">45 minutes</option>
                    <option value="60">60 minutes</option>
                    <option value="75">75 minutes</option>
                    <option value="90">90 minutes</option>
                    <option value="105">105 minutes</option>
                    <option value="120">120 minutes</option>
                    <option value="150">150 minutes</option>
                    <option value="180">180 minutes</option>
                    <option value="210">210 minutes</option>
                    <option value="240">240 minutes</option>
                    <option value="300">300 minutes</option>
                </select>
            </label>
            <button type="submit">Calculate</button>
        </form>
    );
}

function TimePicker({ labelText, time, setTime }) {
    var days = [new Date(), new Date(new Date().getTime() + 24 * 60 * 60 * 1000)];
    if (labelText === "End Time") {
        days.push(new Date(new Date().getTime() + 48 * 60 * 60 * 1000));
    }
    const hours = Array.from({ length: 24 }, (_, i) => i.toString().padStart(2, "0"));
    const minutes = ["00", "15", "30", "45"];
    const options = {
        month: 'short',
        day: 'numeric',
    };

    return (
        <div id="picker">
            <label>{labelText}
                <select value={Math.floor((time % 96) / 4)} onChange={(e) => setTime(Number(e.target.value) * 4 + time % 4 + (Math.floor(time / 96) * 96))}>
                    {hours.filter(m => time !== "192" || m === "00").map((h, i) => (
                        <option key={i} value={i}>{h}</option>
                    ))}
                </select>
                :
                <select value={time % 4} onChange={(e) => setTime(Number(e.target.value) + time - time % 4)}>
                    {minutes.filter(m => time !== "192" || m === "00").map((m, i) => (
                        <option key={i} value={i}>{m}</option>
                    ))}
                </select>
                <select value={Math.floor(time / 96)} onChange={(e) => setTime(Number(e.target.value) * 96 + time % 4 + Math.floor((time % 96) / 4) * 4)}>
                    {days.filter((_, i) => i !== 2 || time % 96 === 0).map((d, i) => (
                        <option key={i} value={i}>{d.toLocaleDateString(options)}</option>
                    ))}
                </select>
            </label>
        </div>
    );
};

function moveStartSlider(newStart, end, duration, setStart) {
    if (Number(newStart) + Number(duration) / 15 <= Number(end)) {
        setStart(newStart)
    }
}

function moveEndSlider(newEnd, start, duration, setEnd) {
    if (Number(newEnd) >= Number(start) + Number(duration) / 15 && Number(newEnd) <= 192) {
        setEnd(newEnd)
    }
}

function changeDuration(newDuration, start, end, setDuration) {
    if (Number(start) + Number(newDuration) / 15 <= Number(end)) {
        setDuration(newDuration)
    }
}

function getDate(numQuarters) {
    const start = new Date();
    start.setUTCHours(0,0,0,0);
    return new Date(start.getTime() + numQuarters * 15 * 60 * 1000);
}

export default ChargeTimeForm;
