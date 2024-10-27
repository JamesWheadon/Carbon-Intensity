import axios from "axios";

const getChargeTime = async () => {
    const response = await axios.get('/api/data');
    return response.data;
};

export default getChargeTime;