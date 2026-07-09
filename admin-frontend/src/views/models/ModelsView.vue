<template>
  <section class="content-grid two-col">
    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Registry</p>
          <h3>模型配置列表</h3>
        </div>
        <button type="button" class="ghost-button" @click="loadModels">
          刷新
        </button>
      </div>

      <div class="search-row">
        <span class="search-input-wrap">
          <input
            v-model="modelSearch"
            type="text"
            placeholder="搜索名称、Provider、模型ID"
            @keydown.enter.prevent="modelSearchTerm = modelSearch"
          />
          <button
            v-if="modelSearch"
            type="button"
            class="search-clear"
            @click="modelSearch = ''; modelSearchTerm = ''"
          >
            <AppIcon name="close" />
          </button>
        </span>
        <button type="button" class="primary-button slim" @click="modelSearchTerm = modelSearch">
          搜索
        </button>
      </div>

      <div v-if="loadingModels" class="empty-copy">正在加载模型...</div>
      <div v-else-if="filteredModels.length" class="list-stack">
        <button
          v-for="item in filteredModels"
          :key="item.id"
          type="button"
          class="list-item"
          :class="{ active: modelForm.id === item.id }"
          @click="applyModel(item)"
        >
          <div>
            <strong>{{ item.displayName }}</strong>
            <span>{{ item.provider }} · {{ item.modelId }}</span>
            <span>{{ item.baseUrl }}</span>
          </div>
          <em>
            {{
              item.defaultModel ? "默认" : item.available ? "可用" : item.enabled ? "异常" : "禁用"
            }}
          </em>
        </button>
      </div>
      <p v-else class="empty-copy">暂无模型配置。</p>
    </article>

    <article class="surface">
      <div class="card-head">
        <div>
          <p class="eyebrow">Editor</p>
          <h3>{{ modelForm.id ? "编辑模型" : "新增模型" }}</h3>
        </div>
        <button type="button" class="text-button" @click="resetModelForm">
          新建
        </button>
      </div>

      <form class="form-grid surface-form" @submit.prevent="saveModel">
        <div
          v-if="modelForm.id && (modelForm.unavailableReason || modelForm.lastCheckMessage)"
          class="status-banner"
          :class="modelForm.available ? 'success' : 'warning'"
        >
          <strong>{{ modelForm.available ? "当前配置可用" : "当前配置不可用" }}</strong>
          <span>
            {{ modelForm.available ? modelForm.lastCheckMessage || "最近一次测试通过" : modelForm.unavailableReason || modelForm.lastCheckMessage }}
          </span>
        </div>

        <div class="form-field">
          <span>Provider</span>
          <div
            class="custom-select"
            :class="{ open: showProviderDropdown }"
          >
            <button
              type="button"
              class="custom-select-trigger"
              @click.stop="showProviderDropdown = !showProviderDropdown"
            >
              <span>{{ modelForm.provider || "自定义" }}</span>
              <span class="custom-select-arrow">&#x2304;</span>
            </button>
            <div
              v-if="showProviderDropdown"
              class="custom-select-menu"
            >
              <button
                v-for="item in providerOptions"
                :key="item"
                type="button"
                @click="selectProvider(item)"
              >
                {{ item }}
              </button>
            </div>
          </div>
        </div>
        <span class="field-hint">{{ activeProviderMeta.summary }}</span>

        <label>
          Base URL
          <input
            v-model="modelForm.baseUrl"
            type="url"
            :placeholder="activeProviderMeta.baseUrlPlaceholder"
          />
          <span class="field-hint">{{ activeProviderMeta.baseUrlHint }}</span>
        </label>

        <label>
          模型 ID
          <input
            v-model="modelForm.modelId"
            type="text"
            :placeholder="activeProviderMeta.modelIdPlaceholder"
          />
          <span class="field-hint">{{ activeProviderMeta.modelIdHint }}</span>
        </label>

        <label>
          显示名称
          <input v-model="modelForm.displayName" type="text" placeholder="例如：DeepSeek V3 生产环境" />
        </label>

        <label>
          API Key
          <input
            v-model="modelForm.apiKey"
            type="password"
            autocomplete="new-password"
            :placeholder="modelForm.apiKeyConfigured ? modelForm.apiKeyMasked || activeProviderMeta.apiKeyPlaceholder : activeProviderMeta.apiKeyPlaceholder"
          />
          <span class="field-hint">
            {{
              modelForm.apiKeyConfigured
                ? `已存在密钥 ${modelForm.apiKeyMasked || ""}，留空表示保持不变。`
                : activeProviderMeta.apiKeyHint
            }}
          </span>
        </label>

        <label>
          启用状态
          <select v-model="modelForm.enabled">
            <option :value="true">启用</option>
            <option :value="false">禁用</option>
          </select>
        </label>

        <label class="checkbox-row">
          <input v-model="modelForm.defaultModel" type="checkbox" :disabled="!modelForm.enabled" />
          <span>设置为默认模型</span>
        </label>

        <p class="form-tip">
          保存时会自动执行一次可用性测试。如果测试失败，系统会提示服务不可用，并自动把该模型配置为禁用状态。
        </p>

        <div class="button-row">
          <button type="submit" class="primary-button" :disabled="savingModel">
            {{ savingModel ? "测试并保存中..." : modelForm.id ? "保存并测试" : "创建并测试" }}
          </button>
          <button
            v-if="modelForm.id"
            type="button"
            class="ghost-button"
            :disabled="savingModel"
            @click="testModel"
          >
            测试
          </button>
          <button
            v-if="modelForm.id"
            type="button"
            class="ghost-button"
            @click="setDefaultModel(modelForm.id)"
          >
            设为默认
          </button>
          <button
            v-if="modelForm.id"
            type="button"
            class="danger-button"
            @click="deleteModel(modelForm.id)"
          >
            删除
          </button>
        </div>
      </form>
    </article>
  </section>
</template>

<script setup>
import { computed, inject, ref } from 'vue';
import AppIcon from '@/components/common/AppIcon.vue';

// 注入共享状态和方法
const adminModels = inject('adminModels');
const loadModels = inject('loadModels');
const showNotice = inject('showNotice');
const requestJson = inject('requestJson');

// Provider 配置
const providerOptions = ["siliconflow", "ollama"];
const providerMeta = {
  siliconflow: {
    summary: "OpenAI 兼容云端接口，需要完整的 Base URL、API Key 和模型 ID。",
    baseUrlPlaceholder: "https://api.siliconflow.cn/v1",
    baseUrlHint: "通常以 /v1 结尾，必须是可访问的完整 HTTP 地址。",
    modelIdPlaceholder: "deepseek-ai/DeepSeek-V3",
    modelIdHint: "填写服务商实际支持的模型标识，区分大小写。",
    apiKeyPlaceholder: "sk-...",
    apiKeyHint: "SiliconFlow 必填。",
  },
  ollama: {
    summary: "本地或私有部署 Ollama 服务，需要可访问的服务地址和已安装的模型 ID。",
    baseUrlPlaceholder: "http://127.0.0.1:11434",
    baseUrlHint: "通常为 Ollama 服务地址，不要追加 /api/chat。",
    modelIdPlaceholder: "qwen2.5:7b",
    modelIdHint: "需与 Ollama 本地已拉取模型名完全一致。",
    apiKeyPlaceholder: "该 provider 通常不需要",
    apiKeyHint: "Ollama 通常不需要 API Key，可留空。",
  },
};

// 本地状态
const loadingModels = ref(false);
const savingModel = ref(false);
const modelSearch = ref("");
const modelSearchTerm = ref("");
const showProviderDropdown = ref(false);
const modelForm = ref(createModelForm());

// 计算属性
const activeProviderMeta = computed(() => {
  return providerMeta[modelForm.value.provider];
});

const filteredModels = computed(() => {
  const q = modelSearchTerm.value.trim().toLowerCase();
  if (!q) return adminModels.value;
  return adminModels.value.filter(
    (item) =>
      (item.displayName || "").toLowerCase().includes(q) ||
      (item.provider || "").toLowerCase().includes(q) ||
      (item.modelId || "").toLowerCase().includes(q),
  );
});

// 工具函数
function createModelForm(item = null) {
  return {
    id: item?.id || null,
    provider: item?.provider || providerOptions[0],
    modelId: item?.modelId || "",
    displayName: item?.displayName || "",
    baseUrl: item?.baseUrl || providerMeta[item?.provider || providerOptions[0]].baseUrlPlaceholder,
    apiKey: "",
    apiKeyConfigured: Boolean(item?.apiKeyConfigured),
    apiKeyMasked: item?.apiKeyMasked || "",
    enabled: item?.enabled ?? true,
    defaultModel: Boolean(item?.defaultModel),
    available: Boolean(item?.available),
    unavailableReason: item?.unavailableReason || "",
    lastCheckedAt: item?.lastCheckedAt || "",
    lastCheckPassed: item?.lastCheckPassed ?? null,
    lastCheckMessage: item?.lastCheckMessage || "",
  };
}

function buildModelSaveNotice(data, savedModel, existedBeforeSave) {
  if (typeof data?.message === "string" && data.message) {
    return data.message;
  }
  if (savedModel?.lastCheckPassed === true) {
    return existedBeforeSave
      ? "模型配置已保存并通过可用性测试。"
      : "模型配置已创建并通过可用性测试。";
  }
  if (savedModel?.lastCheckPassed === false) {
    return `服务不可用，最近一次可用性测试失败：${savedModel.lastCheckMessage || "请检查配置或服务状态。"}`;
  }
  return existedBeforeSave ? "模型配置已保存。" : "模型配置已创建。";
}

function buildModelSaveNoticeType(data, savedModel) {
  if (typeof data?.serviceAvailable === "boolean") {
    return data.serviceAvailable ? "success" : "error";
  }
  if (savedModel?.lastCheckPassed === true) {
    return "success";
  }
  if (savedModel?.lastCheckPassed === false) {
    return "error";
  }
  return "success";
}

function isValidHttpUrl(value) {
  try {
    const url = new URL(String(value || "").trim());
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function validateModelForm() {
  if (!modelForm.value.provider) {
    throw new Error("请选择 provider。");
  }
  if (!modelForm.value.baseUrl || !isValidHttpUrl(modelForm.value.baseUrl)) {
    throw new Error("请输入合法的 Base URL，且必须是 http/https 地址。");
  }
  if (!String(modelForm.value.modelId || "").trim()) {
    throw new Error("请输入模型 ID。");
  }
  if (!String(modelForm.value.displayName || "").trim()) {
    throw new Error("请输入显示名称。");
  }
  if (
    modelForm.value.provider === "siliconflow" &&
    !String(modelForm.value.apiKey || "").trim() &&
    !modelForm.value.apiKeyConfigured
  ) {
    throw new Error("SiliconFlow 必须填写 API Key。");
  }
  if (!modelForm.value.enabled) {
    modelForm.value.defaultModel = false;
  }
}

// 方法
function selectProvider(value) {
  showProviderDropdown.value = false;
  modelForm.value.provider = value;
  modelForm.value.baseUrl = providerMeta[value]?.baseUrlPlaceholder || "";
}

function applyModel(item) {
  modelForm.value = createModelForm(item);
}

function resetModelForm() {
  modelForm.value = createModelForm();
}

async function saveModel() {
  savingModel.value = true;
  try {
    validateModelForm();
    const existedBeforeSave = Boolean(modelForm.value.id);
    const payload = {
      provider: modelForm.value.provider,
      modelId: modelForm.value.modelId,
      displayName: modelForm.value.displayName,
      baseUrl: modelForm.value.baseUrl,
      apiKey: modelForm.value.apiKey,
      enabled: Boolean(modelForm.value.enabled),
      defaultModel: Boolean(modelForm.value.defaultModel),
    };
    const url = modelForm.value.id
      ? `/api/admin/models/${modelForm.value.id}`
      : "/api/admin/models";
    const method = modelForm.value.id ? "PUT" : "POST";
    const data = await requestJson(url, {
      method,
      body: JSON.stringify(payload),
    });
    const savedModel = data?.model || data;
    modelForm.value = createModelForm(savedModel);
    await loadModels();
    showNotice(buildModelSaveNotice(data, savedModel, existedBeforeSave), buildModelSaveNoticeType(data, savedModel));
  } catch (error) {
    console.error('保存模型失败:', error);
    showNotice(error?.message || "保存失败", "error");
  } finally {
    console.log('finally 块执行，重置 savingModel');
    savingModel.value = false;
  }
}

async function testModel() {
  if (!modelForm.value.id) {
    return;
  }
  await saveModel();
}

async function setDefaultModel(id) {
  try {
    const data = await requestJson(`/api/admin/models/${id}/default`, {
      method: "PUT",
    });
    modelForm.value = createModelForm(data);
    await loadModels();
    showNotice("默认模型已更新。", "success");
  } catch (error) {
    showNotice(error.message, "error");
  }
}

async function deleteModel(id) {
  if (!window.confirm("确定删除这条模型配置吗？")) {
    return;
  }
  try {
    await requestJson(`/api/admin/models/${id}`, {
      method: "DELETE",
    });
    resetModelForm();
    await loadModels();
    showNotice("模型已删除。", "success");
  } catch (error) {
    showNotice(error.message, "error");
  }
}
</script>
