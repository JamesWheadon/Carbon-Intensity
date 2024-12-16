function ChargeTimeMessage({ chargeTime, intensityData, duration, comparisonTime }) {
    const saving = getCarbonSaving(chargeTime, intensityData, duration, comparisonTime);
    const savingMessage = saving > 0 ? <h3>Saving: {saving} gCO2/kWh</h3> 
    : saving < 0 ? <h3>Extra Intensity: {Math.abs(saving)} gCO2/kWh</h3> 
    : null;
    return (
        <div>
            <h3>Best Time: {dateTimeToDisplayTime(chargeTime)}</h3>
            {savingMessage}
        </div>
    );
}

export function dateTimeToDisplayTime(dateTime) {
    const h = twoDigitDisplay(dateTime.getHours());
    const m = twoDigitDisplay(dateTime.getMinutes());
    const d = twoDigitDisplay(dateTime.getDate());
    const month = twoDigitDisplay(dateTime.getMonth() + 1);
    return h + ':' + m + " " + d + "/" + month
}

export function getCarbonSaving(chargeTime, intensityData, duration, comparisonDate) {
    const chargeMinutes = Math.floor((chargeTime - intensityData[0].time) / (15 * 60000));
    const currentMinutes = Math.floor((comparisonDate - intensityData[0].time) / (15 * 60000));
    var chargeTotal = 0;
    var currentTotal = 0;
    for (var i = 0; i < duration / 15; i++) {
        chargeTotal += intensityData[Math.floor((chargeMinutes + i) / 2)].intensity;
        currentTotal += intensityData[Math.floor((currentMinutes + i) / 2)].intensity;
    }
    return (currentTotal - chargeTotal) / Math.floor(duration / 15);
}

function twoDigitDisplay(value) {
    return (value < 10) ? "0" + value : value;
}

export default ChargeTimeMessage
