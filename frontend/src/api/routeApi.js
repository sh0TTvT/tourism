import { requestJson } from "./request";

export const routeApi = {
  list() {
    return requestJson("/api/routes");
  },
  get(id) {
    return requestJson(`/api/routes/${id}`);
  },
  delete(id) {
    return requestJson(`/api/routes/${id}`, { method: "DELETE" });
  },
  plan(payload) {
    return requestJson("/api/routes/plan", { method: "POST", body: JSON.stringify(payload) });
  },
  create(payload) {
    return requestJson("/api/routes", { method: "POST", body: JSON.stringify(payload) });
  },
  update(id, payload) {
    return requestJson(`/api/routes/${id}`, { method: "PUT", body: JSON.stringify(payload) });
  },
};
