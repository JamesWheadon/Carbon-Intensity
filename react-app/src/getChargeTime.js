import axios from "axios";

export async function getChargeTime(start, end, duration) {
    const body = {
        "startTime": start,
        "endTime": end,
        "duration": duration
    };
    const response = await axios.post('http://localhost:9000/charge-time', body);
    return response.data;
};

export function timeToDateTime(time) {
    const currentDate = new Date();
    const [hours, minutes] = time.split(':').map(Number);
    currentDate.setHours(hours, minutes, 0, 0);
    return currentDate.toISOString().replace(".000Z", "");
}
