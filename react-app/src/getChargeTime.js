import axios from 'axios';

export async function chargeTime(start, end, duration) {
    const body = createChargeTimeBody(start, end, duration);
    return getChargeTime(body);
}

export async function getChargeTime(body) {
    try {
        const response = await axios.post(`${process.env.REACT_APP_WEB_APP}/charge-time`, body);
        return { chargeTime: new Date(response.data.chargeTime) };
    } catch (error) {
        return { error: "unable to get best charge time" };
    }
}

export function createChargeTimeBody(start, end, duration) {
    const body = {
        'startTime': format(start),
        'endTime': format(end),
        'duration': Number(duration)
    };
    return body;
}

function format(datetime) {
    return datetime.toISOString().replace('.000Z', '');
}
