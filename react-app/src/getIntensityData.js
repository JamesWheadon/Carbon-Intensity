import axios from 'axios';

export async function getIntensityData() {
    const response = await axios.post('http://localhost:9000/intensities');
    return response.data;
}
