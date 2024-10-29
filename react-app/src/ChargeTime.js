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
            {bestTime ? <h3>Best Time: {bestTime}</h3> : null}
        </div>
    );
}

export default ChargeTime