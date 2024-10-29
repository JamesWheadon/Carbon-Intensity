import { useState } from 'react';
import logo from './logo.svg';
import './App.css';
import ChargeTimeForm from './ChargeTimeForm';
import ChargeTime from './ChargeTime';
import { chargeTime } from './getChargeTime';

function App() {
  const [bestTime, setBestTime] = useState(null);

  return (
    <div className="App">
      <header className="App-header">
        <img src={logo} className="App-logo" alt="logo" />
        <h3>When to Wash</h3>
        <p>
          Edit <code>src/App.js</code> and save to reload.
        </p>
        <a
          className="App-link"
          href="https://reactjs.org"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn React
        </a>
      <ChargeTimeForm getChargeTime={ async (start, end, duration) => {
        setBestTime(await chargeTime(start, end, duration))
      }}/>
      {bestTime ? <ChargeTime bestChargeTime={bestTime}/> : null}
      </header>
    </div>
  );
}

export default App;
