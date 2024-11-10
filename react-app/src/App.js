import './App.css';
import ChargeTime from './ChargeTime';
import Barplot from './IntensityGraph';
import { useState } from 'react'

function App() {
    const [data, setData] = useState([
        {"time": "3", "intensity": 150}, 
        {"time": "4", "intensity": 200}
    ]);
	return (
		<div className="App">
			<header className="App-header">
                <Barplot width={400} height={400} data={data} />
				<ChargeTime />
			</header>
		</div>
	);
}

export default App;
