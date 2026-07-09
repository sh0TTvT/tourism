import { requestJson } from "./request";

export const weatherApi = {
  getForecastByCoordinates({ latitude, longitude, location = "当前位置", days = 7 }) {
    const params = new URLSearchParams({
      latitude: String(latitude),
      longitude: String(longitude),
      location,
      days: String(days),
    });
    return requestJson(`/api/weather/forecast?${params.toString()}`);
  },
};
