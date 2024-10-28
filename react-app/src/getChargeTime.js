import axios from "axios";

export async function chargeTime(start, end, duration) {
    const body = createChargeTimeBody(start, end, duration, new Date());
    return getChargeTime(body);
};

export async function getChargeTime(body) {
    const response = await axios.post('http://localhost:9000/charge-time', body);
    const dateTime = new Date(response.data.chargeTime);
    
    var h = dateTime.getHours();
    var m = dateTime.getMinutes();
    h = (h<10) ? '0' + h : h;
    m = (m<10) ? '0' + m : m;
    return h + ':' + m;
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
