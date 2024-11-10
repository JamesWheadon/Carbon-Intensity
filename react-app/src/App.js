import './App.css';
import ChargeTime from './ChargeTime';
import { getIntensityData } from './getIntensityData';
import Barplot from './IntensityGraph';
import { useEffect, useState } from 'react'

function App() {
    const [data, setData] = useState(null);
    var dataGraph = null

    useEffect(() => {
        async function graphData() {
            setData(await getIntensityData())
        }

        graphData()
    }, [])

    if (data) {
        dataGraph = <Barplot data={data} />
    }

    return (
        <div className="App">
            <header className="App-header">
                {dataGraph}
                <ChargeTime />
            </header>
        </div>
    );
}

export default App;
