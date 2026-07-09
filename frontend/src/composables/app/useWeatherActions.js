import { weatherApi } from "../../api/weatherApi";

function readGeolocationError(error) {
  if (!error) return "获取定位失败";
  if (error.code === 1) return "定位权限未开启";
  if (error.code === 2) return "暂时无法获取当前位置";
  if (error.code === 3) return "获取定位超时";
  return error.message || "获取定位失败";
}

function getCurrentPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("当前浏览器不支持定位"));
      return;
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: false,
      maximumAge: 5 * 60 * 1000,
      timeout: 10000,
    });
  });
}

async function resolveLocationLabel(latitude, longitude) {
  try {
    const params = new URLSearchParams({
      format: "jsonv2",
      lat: String(latitude),
      lon: String(longitude),
      "accept-language": "zh-CN",
    });

    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?${params.toString()}`,
    );

    if (!response.ok) {
      return "未知";
    }

    const data = await response.json();
    const address = data?.address || {};

    const city =
      address.city ||
      address.town ||
      address.village ||
      address.municipality ||
      address.county ||
      address.state;

    const district =
      address.city_district ||
      address.district ||
      address.suburb ||
      address.neighbourhood;

    const parts = [city, district].filter(Boolean);
    const uniqueParts = [...new Set(parts)];

    return uniqueParts.length
      ? uniqueParts.join(" · ")
      : data?.display_name?.split(",")?.[0] || "未知";
  } catch {
    return "未知";
  }
}

export function createWeatherActions(ctx) {
  async function loadWeatherForecast() {
    const location = ctx.userLocation.value;
    if (!location?.latitude || !location?.longitude) {
      return;
    }
    ctx.loadingWeather.value = true;
    ctx.weatherError.value = "";
    try {
      ctx.weatherForecast.value = await weatherApi.getForecastByCoordinates({
        latitude: location.latitude,
        longitude: location.longitude,
        location: location.label || "未知",
        days: 7,
      });
    } catch (error) {
      ctx.weatherError.value = error.message || "天气获取失败";
      ctx.weatherForecast.value = null;
    } finally {
      ctx.loadingWeather.value = false;
    }
  }

  async function requestCurrentLocation(options = {}) {
    if (ctx.locatingUser.value) return;
    ctx.locatingUser.value = true;
    ctx.weatherError.value = "";
    try {
      const position = await getCurrentPosition();
      const latitude = position.coords.latitude;
      const longitude = position.coords.longitude;
      const label = await resolveLocationLabel(latitude, longitude);
      ctx.userLocation.value = {
        latitude,
        longitude,
        accuracy: position.coords.accuracy,
        label,
        capturedAt: new Date(position.timestamp || Date.now()).toISOString(),
      };
      await loadWeatherForecast();
      if (options.openPanel) {
        ctx.isWeatherPanelOpen.value = true;
      }
    } catch (error) {
      ctx.weatherError.value = readGeolocationError(error);
    } finally {
      ctx.locatingUser.value = false;
    }
  }

  async function refreshWeather() {
    await requestCurrentLocation({ openPanel: ctx.isWeatherPanelOpen.value });
  }

  async function toggleWeatherPanel() {
    if (ctx.isWeatherPanelOpen.value) {
      ctx.isWeatherPanelOpen.value = false;
      return;
    }
    ctx.isWeatherPanelOpen.value = true;
    if (!ctx.userLocation.value) {
      await requestCurrentLocation({ openPanel: true });
      return;
    }
    if (!ctx.weatherForecast.value && !ctx.loadingWeather.value) {
      await loadWeatherForecast();
    }
  }

  function closeWeatherPanel() {
    ctx.isWeatherPanelOpen.value = false;
  }

  function buildChatLocationPayload() {
    const location = ctx.userLocation.value;
    if (!location?.latitude || !location?.longitude) {
      return null;
    }
    return {
      latitude: location.latitude,
      longitude: location.longitude,
      accuracy: location.accuracy,
      label: location.label || "未知",
      capturedAt: location.capturedAt,
    };
  }

  return {
    loadWeatherForecast,
    requestCurrentLocation,
    refreshWeather,
    toggleWeatherPanel,
    closeWeatherPanel,
    buildChatLocationPayload,
  };
}
