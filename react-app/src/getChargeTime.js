import axios from 'axios';

export async function chargeTime(start, end, duration) {
    const body = createChargeTimeBody(start, end, duration, new Date());
    return getChargeTime(body);
}

export async function getChargeTime(body) {
    try {
        const response = await axios.post('http://localhost:9000/charge-time', body);
        const dateTime = new Date(response.data.chargeTime);

        var h = dateTime.getHours();
        var m = dateTime.getMinutes();
        h = (h < 10) ? '0' + h : h;
        m = (m < 10) ? '0' + m : m;
        return { chargeTime: h + ':' + m };
    } catch (error) {
        return { error: "unable to get best charge time" };
    }
}

export function createChargeTimeBody(start, end, duration, date) {
    const body = {
        'startTime': timeToDateTime(start, date),
        'endTime': timeToDateTime(end, date),
        'duration': Number(duration)
    };
    return body;
}

function timeToDateTime(time, date) {
    const [hours, minutes] = time.split(':').map(Number);
    date.setHours(hours, minutes, 0, 0);
    return date.toISOString().replace('.000Z', '');
}