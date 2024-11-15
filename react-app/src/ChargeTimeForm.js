import { useState } from 'react';
import TimeRange from './TimeRange';
import './ChargeTimeForm.css';

function ChargeTimeForm({ getChargeTime, duration, setDuration }) {
    const [start, setStart] = useState("0");
    const [end, setEnd] = useState("96");
    const submit = (e) => {
        e.preventDefault();
        getChargeTime(getTimeStamp(start), getTimeStamp(end), duration);
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
    const hours = Array.from({ length: 25 }, (_, i) => i.toString().padStart(2, "0"));
    const minutes = ["00", "15", "30", "45"];

    return (
        <div id="picker">
            <label>{labelText}
                <select value={hours[Math.floor(Number(time) / 4)]} onChange={(e) => setTime(Number(e.target.value) * 4 + Number(time) % 4)}>
                    {hours.map((h) => (
                        <option key={h} value={h}>{h}</option>
                    ))}
                </select>
                :
                <select value={minutes[Number(time) % 4]} onChange={(e) => setTime(Number(e.target.value) / 15 + Number(time) - Number(time) % 4)}>
                    {minutes.filter(m => time !== "96" || m === "00").map((m) => (
                        <option key={m} value={m}>{m}</option>
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
    if (Number(newEnd) >= Number(start) + Number(duration) / 15) {
        setEnd(newEnd)
    }
}

function changeDuration(newDuration, start, end, setDuration) {
    if (Number(start) + Number(newDuration) / 15 <= Number(end)) {
        setDuration(newDuration)
    }
}

function getTimeStamp(numQuarters) {
    var h = Math.floor(numQuarters / 4);
    var m = numQuarters % 4 * 15;
    h = (h < 10) ? '0' + h : h;
    m = (m < 10) ? '0' + m : m;
    return h + ':' + m;
}

export default ChargeTimeForm;
