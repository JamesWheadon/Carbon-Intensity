import axios from "axios";
import { getChargeTime, timeToDateTime } from "../getChargeTime";

jest.mock("axios");

test("retrieves charge time for input", async () => {
  const body = {
    "startTime": "20:12",
    "endTime": "20:34",
    "duration": "60"
  };
  axios.post.mockImplementation(() => Promise.resolve({ data: { "chargeTime": "2024-09-30T21:00:00" } }));

  const result = await getChargeTime("20:12", "20:34", "60");

  expect(axios.post).toHaveBeenCalledWith('http://localhost:9000/charge-time', body);
  expect(result).toStrictEqual({ "chargeTime": "2024-09-30T21:00:00" });
});

test("converts time in current day to timestamp", () => {
  const result = timeToDateTime("20:12");

  expect(result).toStrictEqual("2024-10-27T20:12:00");
});
