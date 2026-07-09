// src/utils/request.js
import { storage } from './storage'
import { STORAGE_KEYS } from '@/constants'

function formatError(payload, fallback = "请求失败，请稍后重试。") {
  if (!payload) {
    return fallback;
  }
  if (typeof payload === "string") {
    return payload;
  }
  if (typeof payload.message === "string" && payload.message) {
    return payload.message;
  }
  if (payload.fields && typeof payload.fields === "object") {
    const firstFieldError = Object.values(payload.fields)[0];
    return firstFieldError || fallback;
  }
  return fallback;
}

function clearSession() {
  storage.remove(STORAGE_KEYS.token);
  storage.remove(STORAGE_KEYS.user);
}

export async function requestJson(url, options = {}, withAuth = true) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (withAuth) {
    const token = storage.get(STORAGE_KEYS.token);
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    if (response.status === 401) {
      clearSession();
    }
    throw new Error(formatError(data));
  }

  return data;
}
