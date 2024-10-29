import { useState } from 'react';

function ChargeTimeForm({ getChargeTime }) {
    const [start, setStart] = useState('');
    const [end, setEnd] = useState('');
    const [duration, setDuration] = useState('30');
    const submit = (e) => {
        e.preventDefault();
        getChargeTime(start, end, duration);
    };
    return (
        <form onSubmit={submit}>
            <label>Start time:
                <input
                    type="time"
                    value={start}
                    onChange={(i) => setStart(i.target.value)}
                />
            </label>
            <label>End time:
                <input
                    type="time"
                    value={end}
                    onChange={(i) => setEnd(i.target.value)}
                />
            </label>
            <label> Duration:
                <select
                    value={duration}
                    onChange={(i) => setDuration(i.target.value)}
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
            <button type="submit">Send</button>
        </form>
    );
}

export default ChargeTimeForm;
