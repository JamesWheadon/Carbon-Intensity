import { useState } from 'react';

function ChargeTimeForm() {
    const [start, setStart] = useState("");
    return (
        <form>
            <label>Start time:
                <input
                    type="text"
                    value={start}
                    onChange={(i) => setStart(i.target.value)}
                />
            </label>
        </form>
    );
}

export default ChargeTimeForm;