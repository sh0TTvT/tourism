import { requestJson } from "./request";

export const modelApi = {
  list() {
    return requestJson("/api/models", {}, false);
  },
};
