import axios from "axios";

const getChargeTime = async (start, end, duration) => {
    const body = {
        "startTime": start,
        "endTime": end,
        "duration": duration
    };
    const response = await axios.post('http://localhost:9000/charge-time', body);
    return response.data;
};

export default getChargeTime;