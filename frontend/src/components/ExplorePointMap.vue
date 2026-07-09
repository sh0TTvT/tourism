<template>
  <div class="explore-map-layout">
    <section class="explore-map-card explore-map-canvas-card">
      <div ref="mapElement" class="explore-map-canvas"></div>

      <div class="explore-map-overlay">
        <span class="explore-map-badge">
          <AppIcon name="pin" />
          点击地图选择一个点
        </span>
      </div>

      <div v-if="serviceWarning" class="explore-map-warning">
        <strong>地图服务提示</strong>
        <span>{{ serviceWarning }}</span>
      </div>
    </section>

    <section class="explore-map-card explore-map-detail-card">
      <div v-if="selectedPoint" class="explore-selected-point">
        <strong>{{ selectedPoint.label }}</strong>
        <span>系统会把这个点作为路线讨论的起点。</span>
      </div>
      <div v-else class="explore-selected-point empty">
        <strong>还没有选点</strong>
        <span>先在左侧地图上点击一个位置。</span>
      </div>

      <label class="explore-map-note">
        想补充的需求
        <textarea
          v-model="note"
          rows="5"
          maxlength="240"
          placeholder="例如：想以这里为中心安排半天步行和喝咖啡，不要太赶。"
        ></textarea>
      </label>

      <button
        type="button"
        class="primary-button explore-map-submit"
        :disabled="!selectedPoint"
        @click="emitStartChat"
      >
        开启新对话
      </button>
    </section>
  </div>
</template>

<script setup>
import L from "leaflet";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

import AppIcon from "./AppIcon.vue";

const emit = defineEmits(["start-chat"]);

const defaultMapConfig = {
  tileUrlTemplate: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  attribution: "&copy; OpenStreetMap contributors",
  subdomains: "abc",
  maxZoom: 19,
  defaultCenterLatitude: 31.2304,
  defaultCenterLongitude: 121.4737,
  defaultZoom: 5,
};

const props = defineProps({
  mapConfig: {
    type: Object,
    default: () => ({}),
  },
  serviceWarning: {
    type: String,
    default: "",
  },
});

const mapElement = ref(null);
const selectedPoint = ref(null);
const note = ref("");
const effectiveMapConfig = computed(() => ({
  ...defaultMapConfig,
  ...(props.mapConfig || {}),
}));

let map = null;
let marker = null;
let tileLayer = null;

function pointLabel(latitude, longitude) {
  return `纬度 ${latitude.toFixed(5)} / 经度 ${longitude.toFixed(5)}`;
}

function markerIcon() {
  return L.divIcon({
    className: "explore-point-pin-wrapper",
    html: '<span class="explore-point-pin"><span class="explore-point-pin-core"></span></span>',
    iconSize: [42, 42],
    iconAnchor: [21, 38],
  });
}

function setMarker(latitude, longitude) {
  if (!map) {
    return;
  }

  const latLng = [latitude, longitude];
  selectedPoint.value = {
    latitude,
    longitude,
    label: pointLabel(latitude, longitude),
  };

  if (!marker) {
    marker = L.marker(latLng, {
      icon: markerIcon(),
    }).addTo(map);
  } else {
    marker.setLatLng(latLng);
  }

  marker.bindPopup(`<strong>${selectedPoint.value.label}</strong>`).openPopup();
}

function emitStartChat() {
  if (!selectedPoint.value) {
    return;
  }
  emit("start-chat", {
    ...selectedPoint.value,
    note: note.value.trim(),
  });
}

function syncTileLayer() {
  if (!map) {
    return;
  }
  if (tileLayer) {
    map.removeLayer(tileLayer);
  }
  tileLayer = L.tileLayer(effectiveMapConfig.value.tileUrlTemplate, {
    maxZoom: effectiveMapConfig.value.maxZoom,
    subdomains: effectiveMapConfig.value.subdomains || undefined,
    attribution: effectiveMapConfig.value.attribution,
  }).addTo(map);
}

onMounted(() => {
  if (!mapElement.value) {
    return;
  }

  map = L.map(mapElement.value, {
    zoomControl: true,
    attributionControl: true,
  }).setView(
    [effectiveMapConfig.value.defaultCenterLatitude, effectiveMapConfig.value.defaultCenterLongitude],
    effectiveMapConfig.value.defaultZoom,
  );

  syncTileLayer();

  map.on("click", (event) => {
    setMarker(event.latlng.lat, event.latlng.lng);
  });
});

watch(
  () => props.mapConfig,
  () => {
    if (!map) {
      return;
    }
    syncTileLayer();
    if (!marker) {
      map.setView(
        [effectiveMapConfig.value.defaultCenterLatitude, effectiveMapConfig.value.defaultCenterLongitude],
        effectiveMapConfig.value.defaultZoom,
      );
    }
  },
  { deep: true },
);

onBeforeUnmount(() => {
  if (map) {
    map.remove();
    map = null;
  }
  marker = null;
});
</script>

<style scoped>
.explore-map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(320px, 28vw, 420px);
  gap: 18px;
  width: 100%;
  min-height: 0;
  align-items: stretch;
}

.explore-map-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(248, 250, 252, 0.98) 100%);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.explore-map-canvas-card {
  position: relative;
  overflow: hidden;
  width: 100%;
  min-height: 560px;
}

.explore-map-canvas {
  width: 100%;
  height: 100%;
  min-height: 560px;
}

.explore-map-overlay {
  position: absolute;
  left: 20px;
  top: 20px;
  z-index: 4;
}

.explore-map-warning {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 20px;
  z-index: 4;
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 18px;
  color: #7c2d12;
  background: rgba(255, 247, 237, 0.94);
  border: 1px solid rgba(251, 146, 60, 0.24);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.1);
}

.explore-map-warning strong {
  font-size: 14px;
}

.explore-map-warning span {
  font-size: 13px;
  line-height: 1.6;
}

.explore-map-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  border-radius: 999px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
}

.explore-map-badge svg {
  width: 16px;
  height: 16px;
  color: #0f766e;
}

.explore-map-detail-card {
  display: grid;
  gap: 18px;
  align-content: start;
  padding: 22px;
}

.explore-section-heading p {
  margin: 0 0 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.explore-section-heading h2 {
  margin: 0 0 10px;
  font-size: 30px;
  letter-spacing: -0.04em;
}

.explore-section-heading span {
  color: #475569;
  line-height: 1.7;
}

.explore-selected-point {
  display: grid;
  gap: 6px;
  padding: 18px;
  border-radius: 22px;
  color: #0f172a;
  background: rgba(16, 163, 127, 0.1);
  border: 1px solid rgba(16, 163, 127, 0.14);
}

.explore-selected-point.empty {
  background: rgba(15, 23, 42, 0.035);
  border-color: rgba(15, 23, 42, 0.08);
}

.explore-selected-point span {
  color: #475569;
  line-height: 1.6;
}

.explore-map-note {
  display: grid;
  gap: 8px;
  color: #475569;
}

.explore-map-note textarea {
  min-height: 132px;
}

.explore-map-submit {
  justify-self: flex-start;
}

:deep(.explore-point-pin-wrapper) {
  background: transparent;
  border: 0;
}

:deep(.explore-point-pin) {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  background: rgba(16, 163, 127, 0.16);
  border-radius: 999px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.16);
}

:deep(.explore-point-pin::after) {
  content: "";
  position: absolute;
  inset: 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, #10a37f 0%, #0f766e 100%);
}

:deep(.explore-point-pin-core) {
  position: relative;
  z-index: 1;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.22);
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 14px;
}

:deep(.leaflet-popup-content) {
  margin: 14px 16px;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 1200px) {
  .explore-map-layout {
    grid-template-columns: minmax(0, 1fr) 340px;
  }
}

@media (max-width: 1100px) {
  .explore-map-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .explore-map-canvas-card,
  .explore-map-canvas {
    min-height: 420px;
  }
}
</style>
