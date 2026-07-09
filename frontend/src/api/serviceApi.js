import { requestJson } from "./request";

export const serviceApi = {
  getPublicStatus() {
    return requestJson("/api/public/services/status", {}, false);
  },
};
