import axios from 'axios';

export async function getIntensityData() {
    const response = await axios.post('http://localhost:9000/intensities');
    return intensitiesToTimeData(response.data.intensities, response.data.date);
}

function intensitiesToTimeData(intensities, date) {
    return intensities.map((dataPoint, index) => {
        var h = Math.floor(index / 2);
        const m = index % 2 === 0 ? '00' : '30';
        h = (h < 10) ? '0' + h : h;
        return { time: Date.parse(date.substring(0, 10) + 'T' + h + ':' + m + ':00'), intensity: dataPoint };
    })
}
