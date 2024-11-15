export function dateTimeToDisplayTime(dateTime) {
    var h = dateTime.getHours();
    var m = dateTime.getMinutes();
    h = (h < 10) ? '0' + h : h;
    m = (m < 10) ? '0' + m : m;
    return h + ':' + m
}

export function getCarbonSaving(chargeTime, intensityData, duration) {
    const chargeMinutes = chargeTime.getHours() * 4 + Math.floor(chargeTime.getMinutes() / 15);
    const currentMinutes = new Date(Date.now()).getHours() * 4 + Math.floor(new Date(Date.now()).getMinutes() / 15);
    var chargeTotal = 0;
    var currentTotal = 0;
    for (var i = 0; i < duration / 15; i++) {
        chargeTotal += intensityData[Math.floor((chargeMinutes + i) / 2)].intensity;
        currentTotal += intensityData[Math.floor((currentMinutes + i) / 2)].intensity;
    }
    return (currentTotal - chargeTotal) / Math.floor(duration / 15);
}