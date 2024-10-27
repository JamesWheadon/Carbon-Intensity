import axios from "axios";

export async function getChargeTime(body) {
    const response = await axios.post('http://localhost:9000/charge-time', body);
    return response.data;
};

export function createChargeTimeBody(start, end, duration, date) {
    const body = {
        "startTime": timeToDateTime(start, date),
        "endTime": timeToDateTime(end, date),
        "duration": Number(duration)
    };
    return body;
}

function timeToDateTime(time, date) {
    const [hours, minutes] = time.split(':').map(Number);
    date.setHours(hours, minutes, 0, 0);
    return date.toISOString().replace(".000Z", "");
}
