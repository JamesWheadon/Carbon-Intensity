import { useState } from 'react';
import { chargeTime } from './getChargeTime';
import ChargeTimeForm from './ChargeTimeForm';

function ChargeTime() {
    const [bestTime, setBestTime] = useState(null);

    return (
        <div>
            <ChargeTimeForm getChargeTime={async (start, end, duration) => {
                setBestTime(await chargeTime(start, end, duration))
            }} />
            {bestTime && typeof bestTime !== "object" ? <h3>Best Time: {bestTime}</h3> : null}
            {bestTime && typeof bestTime === "object" && "error" in bestTime ? (
                <h3>Could not get best charge time, please try again</h3>
            ) : null}
        </div>
    );
}

export default ChargeTime
