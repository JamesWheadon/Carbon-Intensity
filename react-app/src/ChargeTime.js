import { useState } from 'react';
import { chargeTime } from './getChargeTime';
import ChargeTimeForm from './ChargeTimeForm';

function ChargeTime() {
    const [bestTime, setBestTime] = useState(null);
    var chargeTimeMessage = null;

    if (bestTime) {
        if ("chargeTime" in bestTime) {
            chargeTimeMessage = <h3>Best Time: {bestTime.chargeTime}</h3>
        } else if ("error" in bestTime) {
            chargeTimeMessage = <h3>Could not get best charge time, please try again</h3>
        }
    }

    return (
        <div>
            <ChargeTimeForm getChargeTime={async (start, end, duration) => {
                setBestTime(await chargeTime(start, end, duration))
            }} />
            {chargeTimeMessage}
        </div>
    );
}

export default ChargeTime
