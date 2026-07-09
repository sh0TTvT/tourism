let authTokenGetter = () => "";
let unauthorizedHandler = null;

export function setApiAuthTokenGetter(getter) {
  authTokenGetter = typeof getter === "function" ? getter : () => "";
}

export function setApiUnauthorizedHandler(handler) {
  unauthorizedHandler = typeof handler === "function" ? handler : null;
}

export function formatApiError(payload, fallback = "请求失败，请稍后重试。") {
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
    return Object.values(payload.fields)[0] || fallback;
  }
  return fallback;
}

function createHeaders(options = {}, withAuth = true) {
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = authTokenGetter();
  if (withAuth && token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return headers;
}

async function parseResponseBody(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function handleUnauthorized(response) {
  if (response.status === 401 && unauthorizedHandler) {
    unauthorizedHandler();
  }
}

export async function requestJson(url, options = {}, withAuth = true) {
  const response = await fetch(url, {
    ...options,
    headers: createHeaders(options, withAuth),
  });

  if (response.status === 204) {
    return null;
  }

  const data = await parseResponseBody(response);

  if (!response.ok) {
    handleUnauthorized(response);
    throw new Error(formatApiError(data));
  }

  return data;
}

export async function requestNdjsonStream(url, options = {}, onEvent, withAuth = true) {
  const response = await fetch(url, {
    ...options,
    headers: createHeaders(options, withAuth),
  });

  if (!response.ok) {
    const data = await parseResponseBody(response);
    handleUnauthorized(response);
    throw new Error(formatApiError(data));
  }

  if (!response.body) {
    throw new Error("当前环境不支持流式响应。");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done });

    let newlineIndex = buffer.indexOf("\n");
    while (newlineIndex !== -1) {
      const line = buffer.slice(0, newlineIndex).trim();
      buffer = buffer.slice(newlineIndex + 1);
      if (line) {
        await onEvent(JSON.parse(line));
      }
      newlineIndex = buffer.indexOf("\n");
    }

    if (done) {
      break;
    }
  }

  const lastLine = buffer.trim();
  if (lastLine) {
    await onEvent(JSON.parse(lastLine));
  }
}
