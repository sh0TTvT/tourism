import { requestJson } from "./request";

export const authApi = {
  login(payload) {
    return requestJson("/api/auth/login", { method: "POST", body: JSON.stringify(payload) }, false);
  },
  register(payload) {
    return requestJson("/api/auth/register", { method: "POST", body: JSON.stringify(payload) }, false);
  },
};
