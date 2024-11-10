import axios from 'axios';

export async function getIntensityData() {
    const response = await axios.post('http://localhost:9000/intensities');
    return intensitiesToTimeData(response.data.intensities);
}

function intensitiesToTimeData(intensities) {
    console.log(typeof intensities);
    return intensities.map((dataPoint, index) => {
        var h = Math.floor(index / 2);
        var m = index % 2 === 0 ? '15' : '45';
        h = (h < 10) ? '0' + h : h;
        m = (m < 10) ? '0' + m : m;
        return { time: h + ':' + m, intensity: dataPoint };
    })
}
