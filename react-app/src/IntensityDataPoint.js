function IntensityDataPoint({ dataPoint }) {
    const startTime = new Date(dataPoint.time)
    const endTime = new Date(dataPoint.time + 30 * 60000)
    const startTimeString = startTime.toTimeString().split(' ')[0].substring(0, 5)
    const endTimeString = endTime.toTimeString().split(' ')[0].substring(0, 5)
    var timeString = null
    if (endTimeString === "00:00") {
        timeString = `${startTimeString} ${startTime.toLocaleDateString("en-GB").substring(0,5)} to ${endTimeString} ${endTime.toLocaleDateString("en-GB").substring(0,5)}`
    } else {
        timeString = `${startTimeString} to ${endTimeString} ${endTime.toLocaleDateString("en-GB").substring(0,5)}`
    }
    return (
        <div>
            <h3>{timeString}</h3>
            <h3>{dataPoint.intensity} gCO2/kWh</h3>
        </div>
    )
}

export default IntensityDataPoint