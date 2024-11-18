export function dateTimeToDisplayTime(dateTime) {
    const h = twoDigitDisplay(dateTime.getHours());
    const m = twoDigitDisplay(dateTime.getMinutes());
    const d = twoDigitDisplay(dateTime.getDate());
    const month = twoDigitDisplay(dateTime.getMonth() + 1);
    return h + ':' + m + " " + d + "/" + month
}

export function getCarbonSaving(chargeTime, intensityData, duration, comparisonDate) {
    console.log(intensityData[0].time)
    console.log(chargeTime)
    const chargeMinutes = Math.floor((chargeTime - intensityData[0].time) / (15 * 60000));
    const currentMinutes = Math.floor((comparisonDate - intensityData[0].time) / (15 * 60000));
    console.log(chargeMinutes);
    console.log(currentMinutes);
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
