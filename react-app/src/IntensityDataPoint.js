function IntensityDataPoint({ dataPoint, clicked, clear }) {
    var clearButton = null;
    if (clicked) {
        clearButton = <button onClick={clear}>clear</button>
    }
    return (
        <div>
            <h3>{new Date(dataPoint.time).toTimeString().split(' ')[0].substring(0, 5)}</h3>
            <h3>{dataPoint.intensity}</h3>
            {clearButton}
        </div>
    )
}

export default IntensityDataPoint