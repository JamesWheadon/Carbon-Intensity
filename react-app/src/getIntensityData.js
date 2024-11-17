import axios from 'axios';

export async function getIntensityData() {
    const response = await axios.post('http://localhost:9000/intensities');
    return intensitiesToTimeData(response.data.intensities, response.data.date);
}

function intensitiesToTimeData(intensities, date) {
    const startDate = new Date(date);
    return intensities.map((dataPoint, index) => {
        return { time: new Date(startDate.getTime() + index * 30 * 60000), intensity: dataPoint };
    })
}
