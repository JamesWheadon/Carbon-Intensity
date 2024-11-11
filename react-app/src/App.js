import './App.css';
import ChargeTime from './ChargeTime';
import { getIntensityData } from './getIntensityData';
import IntensityDataPoint from './IntensityDataPoint';
import Barplot from './IntensityGraph';
import { useEffect, useState } from 'react'

function App() {
    const [data, setData] = useState(null);
    const [dataPoint, setDataPoint] = useState(null);
    var dataGraph = null
    var dataPointDisplay = null

    function clear() {
        setDataPoint(null);
    }

    useEffect(() => {
        async function graphData() {
            setData(await getIntensityData())
        }

        graphData()
    }, [])

    if (data) {
        dataGraph = <Barplot data={data} setDataPoint={setDataPoint}/>
    }

    if (dataPoint) {
        dataPointDisplay = <IntensityDataPoint dataPoint={dataPoint} clear={clear}/>
    }

    return (
        <div className="App">
            <header className="App-header">
                {dataGraph}
                {dataPointDisplay}
                <ChargeTime />
            </header>
        </div>
    );
}

export default App;
