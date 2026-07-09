<template>
  <section class="content-grid two-col">
    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Registry</p>
          <h3>服务配置列表</h3>
        </div>
        <button type="button" class="ghost-button" @click="loadServices">
          刷新
        </button>
      </div>

      <div class="search-row">
        <span class="search-input-wrap">
          <input
            v-model="serviceSearch"
            type="text"
            placeholder="搜索名称、描述"
            @keydown.enter.prevent="serviceSearchTerm = serviceSearch"
          />
          <button
            v-if="serviceSearch"
            type="button"
            class="search-clear"
            @click="serviceSearch = ''; serviceSearchTerm = ''"
          >
            <AppIcon name="close" />
          </button>
        </span>
        <button type="button" class="primary-button slim" @click="serviceSearchTerm = serviceSearch">
          搜索
        </button>
      </div>

      <div v-if="loadingServices" class="empty-copy">正在加载服务配置...</div>
      <div v-else-if="filteredServices.length" class="list-stack">
        <button
          v-for="item in filteredServices"
          :key="item.serviceKey"
          type="button"
          class="list-item"
          :class="{ active: serviceForm.serviceKey === item.serviceKey }"
          @click="applyService(item)"
        >
          <div>
            <strong>{{ item.displayName }}</strong>
            <span>{{ item.description }}</span>
            <span>{{ item.baseUrl }}</span>
          </div>
          <em>
            {{
              item.available ? "可用" : item.enabled ? "异常" : "已关闭"
            }}
          </em>
        </button>
      </div>
      <p v-else class="empty-copy">暂无服务配置。</p>
    </article>

    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Editor</p>
          <h3>{{ serviceForm.displayName || "服务配置" }}</h3>
        </div>
        <button type="button" class="text-button" @click="resetServiceForm">
          重置当前表单
        </button>
      </div>

      <form class="form-grid surface-form" @submit.prevent="saveService">
        <div
          v-if="serviceForm.statusMessage || serviceForm.lastHeartbeatMessage || serviceForm.lastCheckMessage"
          class="status-banner"
          :class="serviceForm.available ? 'success' : 'warning'"
        >
          <strong>{{ serviceForm.available ? "当前服务可用" : "当前服务不可用" }}</strong>
          <span>
            {{
              serviceForm.statusMessage ||
              serviceForm.lastHeartbeatMessage ||
              serviceForm.lastCheckMessage
            }}
          </span>
        </div>

        <label>
          服务类型
          <input :value="activeServiceMeta.label" type="text" disabled />
          <span class="field-hint">{{ activeServiceMeta.summary }}</span>
        </label>

        <label>
          服务名称
          <input v-model="serviceForm.displayName" type="text" maxlength="80" />
        </label>

        <label>
          {{ activeServiceMeta.baseUrlLabel }}
          <input
            v-model="serviceForm.baseUrl"
            :type="activeServiceMeta.baseUrlInputType"
            :placeholder="activeServiceMeta.baseUrlPlaceholder"
          />
          <span class="field-hint">{{ activeServiceMeta.baseUrlHint }}</span>
        </label>

        <label>
          启用状态
          <select v-model="serviceForm.enabled">
            <option :value="true">启用</option>
            <option :value="false">关闭</option>
          </select>
        </label>

        <template v-if="serviceForm.serviceKey === 'weather'">
          <label>
            时区
            <input
              v-model="serviceForm.settings.timezone"
              type="text"
              placeholder="Asia/Shanghai"
            />
            <span class="field-hint">必须是合法 IANA 时区，例如 `Asia/Shanghai`。</span>
          </label>

          <label>
            默认查询天数
            <input
              v-model.number="serviceForm.settings.forecastDays"
              type="number"
              min="1"
              max="16"
            />
            <span class="field-hint">用户未指定天数时的默认天气预报窗口。</span>
          </label>

          <label>
            最大查询天数
            <input
              v-model.number="serviceForm.settings.maxForecastDays"
              type="number"
              min="1"
              max="16"
            />
            <span class="field-hint">天气接口可接受的最大查询窗口，上限建议不超过 16 天。</span>
          </label>
        </template>

        <template v-else>
          <label>
            地图归属说明
            <input
              v-model="serviceForm.settings.attribution"
              type="text"
              placeholder="&copy; OpenStreetMap contributors"
            />
          </label>

          <label>
            子域名
            <input
              v-model="serviceForm.settings.subdomains"
              type="text"
              placeholder="abc"
            />
            <span class="field-hint">如果瓦片地址包含 `{s}`，这里填写可用子域名，例如 `abc`。</span>
          </label>

          <label>
            最大缩放
            <input
              v-model.number="serviceForm.settings.maxZoom"
              type="number"
              min="1"
              max="22"
            />
          </label>

          <label>
            默认中心纬度
            <input
              v-model.number="serviceForm.settings.defaultCenterLatitude"
              type="number"
              step="0.0001"
            />
          </label>

          <label>
            默认中心经度
            <input
              v-model.number="serviceForm.settings.defaultCenterLongitude"
              type="number"
              step="0.0001"
            />
          </label>

          <label>
            默认缩放
            <input
              v-model.number="serviceForm.settings.defaultZoom"
              type="number"
              min="1"
              max="22"
            />
          </label>

          <label>
            地理编码 Base URL
            <input
              v-model="serviceForm.settings.geocodingBaseUrl"
              type="url"
              placeholder="https://nominatim.openstreetmap.org"
            />
            <span class="field-hint">用于路线点位经纬度解析和地图服务心跳探测。</span>
          </label>

          <label>
            地理编码 User-Agent
            <input
              v-model="serviceForm.settings.geocodingUserAgent"
              type="text"
              placeholder="tourism-qa/1.0"
            />
          </label>
        </template>

        <div class="form-tip">
          <div>最近人工测试：{{ formatDateTime(serviceForm.lastCheckedAt) }}</div>
          <div>最近心跳：{{ formatDateTime(serviceForm.lastHeartbeatAt) }}</div>
          <div v-if="serviceForm.lastHeartbeatLatencyMs != null">
            最近心跳耗时：{{ serviceForm.lastHeartbeatLatencyMs }} ms
          </div>
        </div>

        <div class="button-row">
          <button type="submit" class="primary-button" :disabled="savingService">
            {{ savingService ? "保存并测试中..." : "保存并测试" }}
          </button>
          <button
            type="button"
            class="ghost-button"
            :disabled="testingServiceKey === serviceForm.serviceKey"
            @click="testService"
          >
            {{
              testingServiceKey === serviceForm.serviceKey ? "测试中..." : "单独测试"
            }}
          </button>
        </div>
      </form>
    </article>
  </section>
</template>

<script setup>
import { computed, inject, ref } from 'vue';
import AppIcon from '@/components/common/AppIcon.vue';

// Inject 共享状态
const adminServices = inject("adminServices");
const showNotice = inject("showNotice");
const requestJson = inject("requestJson");
const formatDateTime = inject("formatDateTime");

// 本地状态
const loadingServices = ref(false);
const savingService = ref(false);
const testingServiceKey = ref("");
const serviceSearch = ref("");
const serviceSearchTerm = ref("");

// 服务元数据
const serviceMeta = {
  weather: {
    label: "天气服务",
    summary: "管理天气查询接口的基础地址、时区和查询窗口。保存后会真实调用一次天气接口验证可用性。",
    baseUrlLabel: "天气 API Base URL",
    baseUrlPlaceholder: "https://api.open-meteo.com",
    baseUrlHint: "必须是 Open-Meteo 兼容的可访问基础地址，例如 `https://api.open-meteo.com`。",
    baseUrlInputType: "url",
  },
  map: {
    label: "地图服务",
    summary: "统一管理地图瓦片地址和地理编码地址。心跳会同时验证瓦片服务与地理编码服务。",
    baseUrlLabel: "地图瓦片模板",
    baseUrlPlaceholder: "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    baseUrlHint: "建议使用 Leaflet 兼容模板，至少包含 `{z}`、`{x}`、`{y}`，可选 `{s}`。",
    baseUrlInputType: "text",
  },
};

// 默认服务设置
function defaultServiceSettings(serviceKey) {
  if (serviceKey === "map") {
    return {
      attribution: "&copy; OpenStreetMap contributors",
      subdomains: "abc",
      maxZoom: 19,
      defaultCenterLatitude: 31.2304,
      defaultCenterLongitude: 121.4737,
      defaultZoom: 4,
      geocodingBaseUrl: "https://nominatim.openstreetmap.org",
      geocodingUserAgent: "tourism-qa/1.0",
    };
  }
  return {
    timezone: "Asia/Shanghai",
    forecastDays: 3,
    maxForecastDays: 7,
  };
}

// 创建服务表单
function createServiceForm(item = null) {
  const serviceKey = item?.serviceKey || "weather";
  return {
    id: item?.id || null,
    serviceKey,
    displayName: item?.displayName || serviceMeta[serviceKey]?.label || "服务",
    description: item?.description || "",
    enabled: item?.enabled ?? true,
    baseUrl: item?.baseUrl || serviceMeta[serviceKey]?.baseUrlPlaceholder || "",
    settings: {
      ...defaultServiceSettings(serviceKey),
      ...(item?.settings || {}),
    },
    available: item?.available ?? true,
    statusMessage: item?.statusMessage || "",
    lastCheckedAt: item?.lastCheckedAt || "",
    lastCheckPassed: item?.lastCheckPassed ?? null,
    lastCheckMessage: item?.lastCheckMessage || "",
    lastHeartbeatAt: item?.lastHeartbeatAt || "",
    lastHeartbeatPassed: item?.lastHeartbeatPassed ?? null,
    lastHeartbeatMessage: item?.lastHeartbeatMessage || "",
    lastHeartbeatLatencyMs: item?.lastHeartbeatLatencyMs ?? null,
  };
}

const serviceForm = ref(createServiceForm());

// 计算属性
const filteredServices = computed(() => {
  const q = serviceSearchTerm.value.trim().toLowerCase();
  if (!q) return adminServices.value;
  return adminServices.value.filter(
    (item) =>
      (item.displayName || "").toLowerCase().includes(q) ||
      (item.description || "").toLowerCase().includes(q),
  );
});

const activeServiceMeta = computed(
  () => serviceMeta[serviceForm.value.serviceKey] || serviceMeta.weather,
);

// 方法
function applyService(item) {
  serviceForm.value = createServiceForm(item);
}

function resetServiceForm() {
  if (serviceForm.value.serviceKey) {
    const matched = adminServices.value.find((item) => item.serviceKey === serviceForm.value.serviceKey);
    if (matched) {
      serviceForm.value = createServiceForm(matched);
      return;
    }
  }
  serviceForm.value = createServiceForm(adminServices.value[0] || null);
}

function resolveMapProbeUrl(template, subdomains) {
  const seed = String(subdomains || "a").trim() || "a";
  const subdomain = seed.slice(0, 1);
  return String(template || "")
    .replace("{s}", subdomain)
    .replace("{z}", "0")
    .replace("{x}", "0")
    .replace("{y}", "0")
    .replace("{r}", "");
}

function isValidHttpUrl(value) {
  try {
    const url = new URL(String(value || "").trim());
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function validateServiceForm() {
  if (!String(serviceForm.value.displayName || "").trim()) {
    throw new Error("请输入服务名称。");
  }
  if (serviceForm.value.serviceKey === "weather") {
    if (!isValidHttpUrl(serviceForm.value.baseUrl)) {
      throw new Error("请输入合法的天气服务地址。");
    }
    if (!String(serviceForm.value.settings.timezone || "").trim()) {
      throw new Error("请输入天气服务时区。");
    }
    const forecastDays = Number(serviceForm.value.settings.forecastDays);
    const maxForecastDays = Number(serviceForm.value.settings.maxForecastDays);
    if (!Number.isFinite(forecastDays) || forecastDays < 1 || forecastDays > 16) {
      throw new Error("默认天气天数必须在 1 到 16 之间。");
    }
    if (!Number.isFinite(maxForecastDays) || maxForecastDays < forecastDays || maxForecastDays > 16) {
      throw new Error("最大天气天数必须在默认天数到 16 之间。");
    }
    return;
  }

  if (!String(serviceForm.value.baseUrl || "").includes("{z}") || !String(serviceForm.value.baseUrl || "").includes("{x}") || !String(serviceForm.value.baseUrl || "").includes("{y}")) {
    throw new Error("地图瓦片模板至少需要包含 {z}、{x}、{y} 占位符。");
  }
  if (!isValidHttpUrl(resolveMapProbeUrl(serviceForm.value.baseUrl, serviceForm.value.settings.subdomains))) {
    throw new Error("地图瓦片模板无法解析为合法的 HTTP 地址。");
  }
  if (!isValidHttpUrl(serviceForm.value.settings.geocodingBaseUrl)) {
    throw new Error("请输入合法的地理编码 Base URL。");
  }
  if (!String(serviceForm.value.settings.geocodingUserAgent || "").trim()) {
    throw new Error("请输入地理编码 User-Agent。");
  }
  const maxZoom = Number(serviceForm.value.settings.maxZoom);
  const defaultZoom = Number(serviceForm.value.settings.defaultZoom);
  if (!Number.isFinite(maxZoom) || maxZoom < 1 || maxZoom > 22) {
    throw new Error("最大缩放必须在 1 到 22 之间。");
  }
  if (!Number.isFinite(defaultZoom) || defaultZoom < 1 || defaultZoom > maxZoom) {
    throw new Error("默认缩放必须在 1 到最大缩放之间。");
  }
}

async function loadServices() {
  loadingServices.value = true;
  try {
    const data = await requestJson("/api/admin/services");
    adminServices.value = Array.isArray(data) ? data : [];
    if (serviceForm.value.serviceKey) {
      const matched = adminServices.value.find((item) => item.serviceKey === serviceForm.value.serviceKey);
      if (matched) {
        serviceForm.value = createServiceForm(matched);
      } else if (adminServices.value.length) {
        serviceForm.value = createServiceForm(adminServices.value[0]);
      }
    } else if (adminServices.value.length) {
      serviceForm.value = createServiceForm(adminServices.value[0]);
    }
  } catch (error) {
    showNotice(error.message, "error");
  } finally {
    loadingServices.value = false;
  }
}

async function saveService() {
  savingService.value = true;
  try {
    validateServiceForm();
    const payload = {
      displayName: serviceForm.value.displayName,
      enabled: Boolean(serviceForm.value.enabled),
      baseUrl: serviceForm.value.baseUrl,
      settings: serviceForm.value.settings,
    };
    const data = await requestJson(`/api/admin/services/${serviceForm.value.serviceKey}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    const savedService = data?.service || data;
    serviceForm.value = createServiceForm(savedService);
    await loadServices();
    showNotice(data?.message || "服务配置已保存。", data?.serviceAvailable ? "success" : "error");
  } catch (error) {
    showNotice(error.message, "error");
  } finally {
    savingService.value = false;
  }
}

async function testService() {
  testingServiceKey.value = serviceForm.value.serviceKey;
  try {
    const data = await requestJson(`/api/admin/services/${serviceForm.value.serviceKey}/test`, {
      method: "POST",
    });
    await loadServices();
    showNotice(
      data?.available ? data.message || "服务测试通过。" : data?.message || "服务测试失败。",
      data?.available ? "success" : "error",
    );
  } catch (error) {
    showNotice(error.message, "error");
  } finally {
    testingServiceKey.value = "";
  }
}
</script>
