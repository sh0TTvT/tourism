import { requestJson } from "./request";

export const userApi = {
  getProfile() {
    return requestJson("/api/users/me");
  },
  updateProfile(payload) {
    return requestJson("/api/users/me/profile", { method: "PUT", body: JSON.stringify(payload) });
  },
  updatePreferences(payload) {
    return requestJson("/api/users/me/preferences", { method: "PUT", body: JSON.stringify(payload) });
  },
  changePassword(payload) {
    return requestJson("/api/users/me/password", { method: "PUT", body: JSON.stringify(payload) });
  },
};
