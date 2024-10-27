import axios from "axios";
import getChargeTime from "../getChargeTime";

jest.mock("axios");

test("retrieves charge time for input", async () => {
  axios.get.mockImplementation(() => Promise.resolve({ data: { "response": true } }));
  const result = await getChargeTime("20:12", "20:34", "60")
  console.log(result)
  expect(result).toStrictEqual({ "response": true })
});
