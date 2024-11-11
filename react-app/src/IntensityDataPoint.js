function IntensityDataPoint({ dataPoint }) {
    return (
        <div>
            <h3>{new Date(dataPoint.time).toTimeString().split(' ')[0].substring(0, 5)}</h3>
            <h3>{dataPoint.intensity}</h3>
        </div>
    )
}

export default IntensityDataPoint