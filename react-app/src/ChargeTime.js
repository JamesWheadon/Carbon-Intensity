import { useState } from 'react';
import { chargeTime } from './getChargeTime';
import ChargeTimeMessage from './carbonSaving';
import ChargeTimeForm from './ChargeTimeForm';

function ChargeTime({ intensityData }) {
    const [bestTime, setBestTime] = useState(null);
    const [duration, setDuration] = useState("30")
    var chargeTimeMessage = null;

    if (bestTime) {
        if ("chargeTime" in bestTime) {
            chargeTimeMessage = <ChargeTimeMessage  chargeTime={bestTime.chargeTime} intensityData={intensityData} duration={duration} comparisonTime={Date.now()}/>
        } else if ("error" in bestTime) {
            chargeTimeMessage = <h3>Could not get best charge time, please try again</h3>
        }
    }

    return (
        <div>
            <ChargeTimeForm getChargeTime={async (start, end, duration) => {
                setBestTime(await chargeTime(start, end, duration))
            }} duration={duration} setDuration={setDuration} />
            {chargeTimeMessage}
        </div>
    );
}

export default ChargeTime
