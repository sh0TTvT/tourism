<template>
  <div class="route-map-shell" :class="{ 'is-adding-point': isAddingPoint }">
    <div ref="mapElement" class="route-map-canvas"></div>
    <div v-if="serviceWarning" class="route-map-warning">
      <strong>地图服务提示</strong>
      <span>{{ serviceWarning }}</span>
    </div>
    <div v-if="isAddingPoint || !hasCoordinates" class="route-map-empty">
      <strong>{{ isAddingPoint ? "选择点位位置" : "暂无可展示坐标" }}</strong>
      <span>{{
        isAddingPoint
          ? "点击地图后会把新点位加入当前日期。"
          : "生成路线后会在这里展示地图标记和顺序连线。"
      }}</span>
    </div>
  </div>
</template>

<script setup>
import L from "leaflet";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

const defaultMapConfig = {
  tileUrlTemplate: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
  attribution: "&copy; OpenStreetMap contributors",
  subdomains: "abc",
  maxZoom: 19,
  defaultCenterLatitude: 31.2304,
  defaultCenterLongitude: 121.4737,
  defaultZoom: 4,
};

const props = defineProps({
  points: {
    type: Array,
    default: () => [],
  },
  mapConfig: {
    type: Object,
    default: () => ({}),
  },
  serviceWarning: {
    type: String,
    default: "",
  },
  editable: {
    type: Boolean,
    default: false,
  },
  isAddingPoint: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(["map-click", "point-click", "point-drag"]);

const mapElement = ref(null);
const effectiveMapConfig = computed(() => ({
  ...defaultMapConfig,
  ...(props.mapConfig || {}),
}));
const hasCoordinates = computed(() =>
  props.points.some(
    (point) => Number.isFinite(point?.latitude) && Number.isFinite(point?.longitude),
  ),
);

const dayColors = ["#0f766e", "#2563eb", "#d97706", "#be123c", "#7c3aed", "#0f766e"];

let map = null;
let markersLayer = null;
let polylinesLayer = null;
let tileLayer = null;

function normalizedGroups() {
  const grouped = new Map();
  props.points
    .filter((point) => Number.isFinite(point?.latitude) && Number.isFinite(point?.longitude))
    .sort((left, right) => left.day - right.day || left.order - right.order)
    .forEach((point, index) => {
      if (!grouped.has(point.day)) {
        grouped.set(point.day, []);
      }
      grouped.get(point.day).push({ ...point, globalOrder: index + 1 });
    });
  return [...grouped.entries()].map(([day, points]) => ({ day, points }));
}

function clearLayers() {
  markersLayer?.clearLayers();
  polylinesLayer?.clearLayers();
}

function pinIcon(label, color) {
  return L.divIcon({
    className: "route-map-pin-wrapper",
    html: `<span class="route-map-pin" style="--pin-color:${color}"><span class="route-map-pin-label">${label}</span></span>`,
    iconSize: [34, 34],
    iconAnchor: [17, 34],
    popupAnchor: [0, -28],
  });
}

function renderMap() {
  console.log("[DEBUG] renderMap 开始");
  if (!map) {
    console.log("[DEBUG] map 未初始化");
    return;
  }

  clearLayers();

  const groups = normalizedGroups();
  console.log("[DEBUG] normalizedGroups:", groups);

  const bounds = [];
  groups.forEach((group, groupIndex) => {
    const color = dayColors[groupIndex % dayColors.length];
    const latLngs = group.points.map((point) => [point.latitude, point.longitude]);

    if (latLngs.length > 1) {
      L.polyline(latLngs, {
        color,
        weight: 4,
        opacity: 0.76,
        lineCap: "round",
        lineJoin: "round",
      }).addTo(polylinesLayer);
    }

    group.points.forEach((point) => {
      const latLng = [point.latitude, point.longitude];
      bounds.push(latLng);
      const marker = L.marker(latLng, {
        icon: pinIcon(point.globalOrder, color),
        draggable: props.editable,
        autoPan: true,
      })
        .bindPopup(
          `<strong>Day ${point.day} · ${point.name}</strong><br/>${point.description || "未填写说明"}`,
        )
        .addTo(markersLayer);

      marker.on("click", (event) => {
        if (event.originalEvent) {
          L.DomEvent.stopPropagation(event.originalEvent);
        }
        emit("point-click", {
          index: point.flatIndex,
          day: point.day,
          order: point.order,
        });
      });

      marker.on("dragend", (event) => {
        if (!props.editable) {
          return;
        }
        const nextLatLng = event.target.getLatLng();
        emit("point-drag", {
          index: point.flatIndex,
          day: point.day,
          order: point.order,
          latitude: nextLatLng.lat,
          longitude: nextLatLng.lng,
        });
      });
    });
  });

  console.log("[DEBUG] bounds:", bounds, "bounds.length:", bounds.length);

  if (bounds.length) {
    map.fitBounds(bounds, {
      padding: [40, 40],
      maxZoom: 13,
    });
    console.log("[DEBUG] renderMap 完成 - 已渲染", bounds.length, "个标记");
    return;
  }

  map.setView(
    [effectiveMapConfig.value.defaultCenterLatitude, effectiveMapConfig.value.defaultCenterLongitude],
    effectiveMapConfig.value.defaultZoom,
  );
  console.log("[DEBUG] renderMap 完成 - 无标记，使用默认视图");
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

async function syncMap() {
  if (!map) {
    return;
  }
  await nextTick();
  map.invalidateSize();
  renderMap();
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

  markersLayer = L.layerGroup().addTo(map);
  polylinesLayer = L.layerGroup().addTo(map);
  map.on("click", (event) => {
    emit("map-click", {
      latitude: event.latlng.lat,
      longitude: event.latlng.lng,
    });
  });
  syncMap();
});

watch(
  () => props.points,
  (newPoints) => {
    console.log("[DEBUG] RouteMap watch 触发, points:", newPoints?.map((p) => ({
      name: p.name,
      lat: p.latitude,
      lon: p.longitude,
      hasCoords: Number.isFinite(p?.latitude) && Number.isFinite(p?.longitude),
    })));
    syncMap();
  },
  { deep: true },
);

watch(
  () => props.mapConfig,
  async () => {
    if (!map) {
      return;
    }
    syncTileLayer();
    await syncMap();
  },
  { deep: true },
);

onBeforeUnmount(() => {
  if (map) {
    map.remove();
    map = null;
  }
});
</script>

<style scoped>
.route-map-shell {
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  min-height: 360px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, rgba(248, 250, 252, 0.96) 100%);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.route-map-canvas {
  flex: 1 1 auto;
  width: 100%;
  height: 100%;
  min-height: 0;
}

.route-map-empty {
  position: absolute;
  inset: auto 20px 20px 20px;
  z-index: 500;
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 18px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  pointer-events: none;
}

.route-map-warning {
  position: absolute;
  left: 20px;
  right: 20px;
  top: 20px;
  z-index: 500;
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 18px;
  color: #7c2d12;
  background: rgba(255, 247, 237, 0.94);
  border: 1px solid rgba(251, 146, 60, 0.24);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.1);
}

.route-map-warning strong {
  font-size: 14px;
}

.route-map-warning span {
  font-size: 13px;
  line-height: 1.6;
}

.route-map-empty strong {
  font-size: 14px;
}

.route-map-empty span {
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

:deep(.route-map-pin-wrapper) {
  background: transparent;
  border: 0;
}

:deep(.route-map-pin) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  background: var(--pin-color);
  border: 3px solid rgba(255, 255, 255, 0.96);
  border-radius: 999px 999px 999px 0;
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.16);
  transform: rotate(-45deg);
}

:deep(.route-map-pin-label) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transform: rotate(45deg);
  text-shadow: 0 1px 2px rgba(15, 23, 42, 0.2);
}

.route-map-shell.is-adding-point :deep(.leaflet-container) {
  cursor: crosshair;
}

:deep(.leaflet-marker-draggable) {
  cursor: grab;
}

:deep(.leaflet-marker-dragging) {
  cursor: grabbing;
}

:deep(.leaflet-popup-content-wrapper) {
  border-radius: 14px;
}

:deep(.leaflet-popup-content) {
  margin: 14px 16px;
  color: #0f172a;
  font-size: 13px;
  line-height: 1.6;
}
</style>
