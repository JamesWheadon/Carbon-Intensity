import { useState, useRef, useEffect } from 'react';
import './TimeRange.css';

function TimeRange({ start, end, moveStart, moveEnd }) {
    const slider = useRef(null);
    const [left, setLeft] = useState(0);
    const [width, setWidth] = useState(0);

    useEffect(() => {
        sliderCentre(slider, start, end, setLeft, setWidth);
    }, [start, end]);

    return (
        <span id="double-range">
            <input type="range" min="0" max="96" value={start} onChange={(i) => moveStart(i.target.value)} className="time-range" id="lower-range" ref={slider}></input>
            <input type="range" min="0" max="96" value={end} onChange={(i) => moveEnd(i.target.value)} className="time-range" id="upper-range"></input>
            <div style={{ left: left, width: width }} />
        </span>
    );
}

function sliderCentre(slider, start, end, setLeft, setWidth) {
    const sliderRect = slider.current.getBoundingClientRect();
    setLeft(-135 + (sliderRect.width - 50) * (start / 96));
    setWidth((sliderRect.width - 50) * ((end - start) / 96));
}

export default TimeRange;
