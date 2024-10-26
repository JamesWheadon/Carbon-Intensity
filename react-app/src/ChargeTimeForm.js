import { useState } from 'react';

function ChargeTimeForm() {
    const [start, setStart] = useState("");
    const [end, setEnd] = useState("");
    return (
        <form>
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
        </form>
    );
}

export default ChargeTimeForm;