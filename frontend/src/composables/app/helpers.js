import { STORAGE_KEYS } from "../../constants/uiOptions";

export function createDefaultMapConfig() {
  return {
    enabled: true,
    tileUrlTemplate: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    attribution: "&copy; OpenStreetMap contributors",
    subdomains: "abc",
    maxZoom: 19,
    defaultCenterLatitude: 31.2304,
    defaultCenterLongitude: 121.4737,
    defaultZoom: 4,
  };
}

export function parseStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEYS.user) || "null");
  } catch {
    return null;
  }
}

export function resolveViewFromHash() {
  if (window.location.hash.includes("profile")) {
    return "profile";
  }
  if (window.location.hash.includes("explore")) {
    return "explore";
  }
  if (window.location.hash.includes("routes")) {
    return "routes";
  }
  return "chat";
}

export function formatError(payload, fallback = "请求失败，请稍后重试。") {
  if (!payload) return fallback;
  if (typeof payload === "string") return payload;
  if (typeof payload.message === "string" && payload.message) return payload.message;
  if (payload.fields && typeof payload.fields === "object") {
    return Object.values(payload.fields)[0] || fallback;
  }
  return fallback;
}

export function resolveElement(target) {
  return target?.value || target || null;
}
