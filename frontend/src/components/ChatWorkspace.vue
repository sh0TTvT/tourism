<template>
  <header class="chat-toolbar">
    <div class="toolbar-left">
      <button
        v-if="isMobileViewport"
        type="button"
        class="icon-button mobile-nav-button"
        data-tooltip="打开边栏"
        title="打开边栏"
        aria-label="打开边栏"
        @click="emit('open-mobile-sidebar')"
      >
        <AppIcon name="panel-open" />
      </button>

      <div
        ref="modelMenuRoot"
        class="model-select"
        :class="{ open: isModelMenuOpen }"
      >
        <button
          type="button"
          class="model-select-trigger"
          :disabled="!models.length"
          :aria-expanded="isModelMenuOpen"
          aria-haspopup="listbox"
          @click="emit('toggle-model-menu')"
        >
          <div class="model-trigger-copy">
            <strong>{{ currentModelTitle }}</strong>
            <span>{{ currentModelInlineMeta }}</span>
          </div>
          <AppIcon
            name="chevron-down"
            class="select-chevron"
            :class="{ open: isModelMenuOpen }"
          />
        </button>

        <transition name="fade-slide">
          <div v-if="isModelMenuOpen" class="model-menu">
            <section class="model-menu-section">
              <p class="model-menu-label">推荐模型</p>
              <button
                v-for="item in availableModels"
                :key="`available-${item.provider}-${item.id}`"
                type="button"
                class="model-option"
                :class="{ selected: isSelectedModel(item) }"
                @click="emit('select-model-option', item)"
              >
                <span class="model-option-copy">
                  <strong>
                    {{ item.displayName }}
                    <em v-if="isDefaultModel(item)">默认</em>
                  </strong>
                  <span>{{ modelOptionSubtitle(item) }}</span>
                </span>
                <AppIcon
                  v-if="isSelectedModel(item)"
                  name="check"
                  class="model-option-check"
                />
              </button>
            </section>

            <section
              v-if="unavailableModels.length"
              class="model-menu-section muted"
            >
              <p class="model-menu-label">暂不可用</p>
              <div
                v-for="item in unavailableModels"
                :key="`unavailable-${item.provider}-${item.id}`"
                class="model-option disabled"
              >
                <span class="model-option-copy">
                  <strong>{{ item.displayName }}</strong>
                  <span>{{ modelOptionSubtitle(item) }}</span>
                </span>
              </div>
            </section>
          </div>
        </transition>
      </div>
    </div>

    <div ref="weatherPanelRoot" class="toolbar-right">
      <div class="weather-widget" :class="{ open: isWeatherPanelOpen }">
        <button
          type="button"
          class="weather-trigger"
          :class="{ empty: !todayWeather && !weatherError }"
          :aria-expanded="isWeatherPanelOpen"
          aria-haspopup="dialog"
          @click="emit('toggle-weather-panel')"
        >
          <span class="weather-trigger-icon" :class="weatherIconTone(todayWeather)">
            <AppIcon :name="weatherIconName(todayWeather)" />
          </span>
          <span class="weather-trigger-copy">
            <strong>{{ weatherTriggerTitle }}</strong>
            <span>{{ weatherTriggerMeta }}</span>
          </span>
        </button>

        <transition name="fade-slide">
          <section
            v-if="isWeatherPanelOpen"
            class="weather-popover"
            role="dialog"
            aria-label="一周天气"
          >
            <div class="weather-popover-head">
              <div>
                <p>{{ weatherLocationLabel }}</p>
                <strong>未来一周</strong>
              </div>
              <button
                type="button"
                class="icon-button weather-refresh-button"
                :disabled="weatherRefreshDisabled"
                data-tooltip="刷新天气"
                title="刷新天气"
                aria-label="刷新天气"
                @click.stop="emit('refresh-weather')"
              >
                <AppIcon name="refresh" />
              </button>
            </div>

            <div v-if="locatingUser || loadingWeather" class="weather-panel-state">
              {{ locatingUser ? "正在获取定位" : "正在更新天气" }}
            </div>
            <div v-else-if="weatherDisplayError" class="weather-panel-state error">
              {{ weatherDisplayError }}
            </div>
            <div v-else-if="weatherItems.length" class="weather-week-list">
              <article
                v-for="item in weatherItems"
                :key="item.date"
                class="weather-day-row"
              >
                <span class="weather-day-date">{{ formatWeatherDate(item.date) }}</span>
                <strong class="weather-day-condition">
                  <span class="weather-day-icon" :class="weatherIconTone(item)">
                    <AppIcon :name="weatherIconName(item)" />
                  </span>
                  <span>{{ item.weatherDescription || "天气未知" }}</span>
                </strong>
                <span>{{ formatTemperatureRange(item) }}</span>
              </article>
            </div>
            <div v-else class="weather-panel-state">
              暂无天气数据
            </div>
          </section>
        </transition>
      </div>
    </div>
  </header>

  <section ref="messageScroller" class="message-stage">
    <div v-if="!messages.length" class="welcome-panel">
      <div class="welcome-copy">
        <h2>新建一场旅游对话</h2>
      </div>

      <div class="starter-grid">
        <button
          v-for="prompt in starterPrompts"
          :key="prompt.title"
          type="button"
          class="starter-card"
          @click="emit('use-starter-prompt', prompt.prompt)"
        >
          <span class="starter-icon">
            <AppIcon :name="prompt.icon" />
          </span>
          <strong>{{ prompt.title }}</strong>
          <span>{{ prompt.description }}</span>
        </button>
      </div>
    </div>

    <div v-else class="message-list">
      <article
        v-for="(message, index) in messages"
        :key="`${message.role}-${index}-${message.createdAt || index}`"
        class="message-row"
        :class="[message.role, { streaming: message.streaming }]"
      >
        <div class="message-body">
          <div class="message-meta">
            <strong>{{ message.role === "assistant" ? assistantName : userDisplayName }}</strong>
            <span>{{ formatMessageTime(message.createdAt) }}</span>

            <div
              v-if="message.role === 'assistant' && assistantSourceItems(message).length"
              class="message-source-menu"
            >
              <button
                type="button"
                class="message-source-trigger"
                :aria-expanded="activeSourceMessageKey === messageSourceKey(message, index)"
                @click.stop="toggleMessageSources(message, index)"
              >
                数据来源
                <span aria-hidden="true">&gt;</span>
              </button>

              <transition name="fade-slide">
                <div
                  v-if="activeSourceMessageKey === messageSourceKey(message, index)"
                  class="message-source-popover"
                  role="dialog"
                  aria-label="数据来源"
                  @click.stop
                >
                  <div class="message-source-popover-head">数据来源</div>
                  <ol class="message-source-list">
                    <li
                      v-for="(source, sourceIndex) in assistantSourceItems(message)"
                      :key="`${messageSourceKey(message, index)}-source-${sourceIndex}`"
                    >
                      <a
                        v-if="source.url"
                        :href="source.url"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {{ source.title || source.url }}
                      </a>
                      <span v-else>{{ source.title }}</span>
                    </li>
                  </ol>
                </div>
              </transition>
            </div>
          </div>

          <div v-if="message.role === 'user'" class="message-bubble">
            <p class="message-content">{{ message.content }}</p>
          </div>
          <div v-else class="message-answer">
            <div
              v-if="message.content"
              class="message-markdown"
              v-html="renderAssistantMessage(assistantDisplayContent(message))"
            ></div>
            <p v-else class="message-content">
              {{ message.recovering ? "连接中断，正在等待完整回答..." : message.streaming ? "正在生成..." : "" }}
            </p>
            <span
              v-if="message.streaming || message.recovering"
              class="message-streaming-caret"
            ></span>
          </div>

          <div
            v-if="message.role === 'assistant' && !message.streaming && !message.recovering && message.content && isLastAssistantMessage(index)"
            class="message-actions"
          >
            <button
              type="button"
              class="message-action-button"
              title="复制回答"
              @click="handleCopyMessage(message)"
            >
              <AppIcon name="copy" />
            </button>
            <button
              type="button"
              class="message-action-button"
              title="重新生成"
              @click="handleRegenerateMessage(index)"
            >
              <AppIcon name="refresh" />
            </button>
          </div>

          <div
            v-if="message.role === 'assistant' && message.suggestRoute"
            class="message-route-actions"
          >
            <span class="message-chip">这轮回答适合继续生成路线</span>
            <button
              type="button"
              class="ghost-button message-route-button"
              :disabled="routeGenerating"
              @click="emit('open-route-draft', message)"
            >
              {{ routeGenerating ? "生成中..." : "生成路线" }}
            </button>
          </div>

          <div
            v-if="message.role === 'assistant' && assistantContextTags(message).length"
            class="message-context-tags"
          >
            <span
              v-for="tag in assistantContextTags(message)"
              :key="tag.key"
              class="message-context-chip"
              :class="tag.tone"
            >
              {{ tag.label }}
            </span>
          </div>
        </div>
      </article>
    </div>
  </section>

  <form class="composer-panel" @submit.prevent="handleComposerSubmit">
    <div class="composer-shell" :class="{ expanded: isComposerExpanded }">
      <div class="composer-input-wrap">
        <span class="composer-leading-icon">
          <AppIcon name="sparkles" />
        </span>
        <textarea
          ref="composerInput"
          :value="chatInput"
          rows="1"
          :disabled="composerDisabled"
          :placeholder="composerPlaceholder"
          @input="updateChatInput"
          @keydown="handleComposerKeydown"
        ></textarea>
      </div>

      <div class="composer-actions">
        <button
          type="submit"
          class="send-button"
          :class="{ stop: isStreaming }"
          :disabled="sendButtonDisabled"
          :title="isStreaming ? '停止流式传输' : '发送消息'"
          :aria-label="isStreaming ? '停止流式传输' : '发送消息'"
        >
          <template v-if="isStreaming">
            <AppIcon name="stop" />
            <span>停止</span>
          </template>
          <AppIcon v-else name="send" />
        </button>
      </div>
    </div>
  </form>
</template>

<script setup>
import MarkdownIt from "markdown-it";
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";

import AppIcon from "./AppIcon.vue";

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
});

const defaultLinkOpen =
  markdown.renderer.rules.link_open ||
  ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options));

markdown.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  token.attrSet("target", "_blank");
  token.attrSet("rel", "noopener noreferrer");
  return defaultLinkOpen(tokens, idx, options, env, self);
};

const props = defineProps({
  isMobileViewport: {
    type: Boolean,
    default: false,
  },
  isModelMenuOpen: {
    type: Boolean,
    default: false,
  },
  models: {
    type: Array,
    default: () => [],
  },
  availableModels: {
    type: Array,
    default: () => [],
  },
  unavailableModels: {
    type: Array,
    default: () => [],
  },
  provider: {
    type: String,
    default: "",
  },
  model: {
    type: String,
    default: "",
  },
  defaultProvider: {
    type: String,
    default: "",
  },
  defaultModel: {
    type: String,
    default: "",
  },
  messages: {
    type: Array,
    default: () => [],
  },
  starterPrompts: {
    type: Array,
    default: () => [],
  },
  userDisplayName: {
    type: String,
    default: "未登录",
  },
  chatInput: {
    type: String,
    default: "",
  },
  composerDisabled: {
    type: Boolean,
    default: false,
  },
  sendButtonDisabled: {
    type: Boolean,
    default: false,
  },
  isStreaming: {
    type: Boolean,
    default: false,
  },
  composerPlaceholder: {
    type: String,
    default: "",
  },
  routeGenerating: {
    type: Boolean,
    default: false,
  },
  weatherServiceWarning: {
    type: String,
    default: "",
  },
  weatherForecast: {
    type: Object,
    default: null,
  },
  userLocation: {
    type: Object,
    default: null,
  },
  locatingUser: {
    type: Boolean,
    default: false,
  },
  loadingWeather: {
    type: Boolean,
    default: false,
  },
  weatherError: {
    type: String,
    default: "",
  },
  isWeatherPanelOpen: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  "open-mobile-sidebar",
  "open-route-draft",
  "select-model-option",
  "send-message",
  "stop-streaming",
  "toggle-model-menu",
  "toggle-weather-panel",
  "close-weather-panel",
  "refresh-weather",
  "update:chatInput",
  "use-starter-prompt",
  "copy-message",
  "regenerate-message",
]);

const composerInput = ref(null);
const messageScroller = ref(null);
const modelMenuRoot = ref(null);
const weatherPanelRoot = ref(null);
const isComposerExpanded = ref(false);
const activeSourceMessageKey = ref(null);

let scrollFrameId = 0;

const selectedModel = computed(
  () =>
    props.models.find(
      (item) => item.provider === props.provider && item.id === props.model,
    ) || null,
);

const assistantName = computed(
  () => selectedModel.value?.displayName || "旅游助手",
);

const currentModelTitle = computed(
  () => selectedModel.value?.displayName || "选择模型",
);

const currentModelInlineMeta = computed(() => {
  if (!props.models.length) {
    return "加载中";
  }
  if (!selectedModel.value) {
    return "未选择";
  }
  if (!selectedModel.value.available) {
    return "不可用";
  }
  return isDefaultModel(selectedModel.value)
    ? "默认"
    : formatProviderLabel(selectedModel.value.provider);
});

const weatherItems = computed(() =>
  Array.isArray(props.weatherForecast?.forecasts)
    ? props.weatherForecast.forecasts.slice(0, 7)
    : [],
);

const todayWeather = computed(() => weatherItems.value[0] || null);

const weatherLocationLabel = computed(
  () => props.weatherForecast?.location || props.userLocation?.label || "当前位置",
);

const weatherTriggerTitle = computed(() => {
  if (props.locatingUser) return "定位中";
  if (props.loadingWeather) return "更新中";
  if (weatherDisplayError.value) return "天气不可用";
  if (!todayWeather.value) return "获取天气";
  return todayWeather.value.weatherDescription || "天气未知";
});

const weatherDisplayError = computed(
  () => props.weatherServiceWarning || props.weatherError,
);

const weatherRefreshDisabled = computed(
  () => props.locatingUser || props.loadingWeather || Boolean(props.weatherServiceWarning),
);

const weatherTriggerMeta = computed(() => {
  if (weatherDisplayError.value) return weatherDisplayError.value;
  if (!todayWeather.value) return weatherLocationLabel.value;
  return formatTemperatureRange(todayWeather.value);
});

function formatProviderLabel(value) {
  const providerMap = {
    siliconflow: "SiliconFlow",
    ollama: "Ollama",
  };
  return providerMap[value] || String(value || "");
}

function isDefaultModel(item) {
  return item?.provider === props.defaultProvider && item?.id === props.defaultModel;
}

function isSelectedModel(item) {
  return item?.provider === props.provider && item?.id === props.model;
}

function modelOptionSubtitle(item) {
  if (!item?.available) {
    return item?.reason || `${formatProviderLabel(item?.provider)} 当前不可用`;
  }
  if (isDefaultModel(item)) {
    return `默认推荐 · ${formatProviderLabel(item.provider)}`;
  }
  return formatProviderLabel(item.provider);
}

function updateChatInput(event) {
  emit("update:chatInput", event.target.value);
}

function formatMessageTime(value) {
  if (!value) {
    return "刚刚";
  }
  try {
    return new Intl.DateTimeFormat("zh-CN", {
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(value));
  } catch {
    return "刚刚";
  }
}

function formatTemperatureRange(item) {
  const min = Number.isFinite(item?.minTemperature) ? Math.round(item.minTemperature) : null;
  const max = Number.isFinite(item?.maxTemperature) ? Math.round(item.maxTemperature) : null;
  if (min == null && max == null) return "气温未知";
  if (min == null) return `${max}°C`;
  if (max == null) return `${min}°C`;
  return `${min}-${max}°C`;
}

function weatherDescriptionOf(item) {
  return String(item?.weatherDescription || "").trim();
}

function weatherIconName(item) {
  const text = weatherDescriptionOf(item);
  if (!text) return "cloud-sun";
  if (/雷|暴/.test(text)) return "cloud-lightning";
  if (/雪|冰雹|霰/.test(text)) return "cloud-snow";
  if (/雨|阵雨|小雨|中雨|大雨|降水/.test(text)) return "cloud-rain";
  if (/雾|霾|沙尘|浮尘/.test(text)) return "fog";
  if (/晴/.test(text)) return "sun";
  if (/阴|云|多云/.test(text)) return "cloud";
  return "cloud-sun";
}

function weatherIconTone(item) {
  const icon = weatherIconName(item);
  return {
    sunny: icon === "sun",
    cloudy: icon === "cloud" || icon === "cloud-sun",
    rainy: icon === "cloud-rain",
    snowy: icon === "cloud-snow",
    stormy: icon === "cloud-lightning",
    foggy: icon === "fog",
  };
}

function formatWeatherDate(value) {
  if (!value) return "";
  try {
    return new Intl.DateTimeFormat("zh-CN", {
      month: "numeric",
      day: "numeric",
      weekday: "short",
    }).format(new Date(`${value}T00:00:00`));
  } catch {
    return value;
  }
}

function messageSourceKey(message, index) {
  return `${message?.role || "message"}-${message?.createdAt || index}-${index}`;
}

function cleanSourceLine(value) {
  return String(value || "")
    .trim()
    .replace(/^[-*•\d.、)）\s]+/, "")
    .trim();
}

function normalizeSourceUrl(value) {
  const text = String(value || "").trim();
  if (!text) return "";
  if (/^https?:\/\//i.test(text)) return text;
  if (/^www\./i.test(text)) return `https://${text}`;
  return text;
}

function parseSourceLine(value) {
  const line = cleanSourceLine(value);
  if (!line) return null;

  const markdownLink = line.match(/\[([^\]]+)\]\((https?:\/\/[^)\s]+|www\.[^)\s]+)\)/i);
  if (markdownLink) {
    return {
      title: cleanSourceLine(line.replace(markdownLink[0], markdownLink[1])) || markdownLink[1],
      url: normalizeSourceUrl(markdownLink[2]),
    };
  }

  const rawUrl = line.match(/(https?:\/\/[^\s)）]+|www\.[^\s)）]+)/i);
  if (rawUrl) {
    const title = cleanSourceLine(line.replace(rawUrl[0], "")) || rawUrl[0];
    return {
      title,
      url: normalizeSourceUrl(rawUrl[0]),
    };
  }

  return { title: line, url: "" };
}

function normalizeSourceItem(item) {
  if (item == null) return null;
  if (typeof item === "string") {
    return parseSourceLine(item);
  }
  if (typeof item === "object") {
    const title = String(
      item.title ||
        item.name ||
        item.label ||
        item.source ||
        item.content ||
        item.text ||
        item.url ||
        "",
    ).trim();
    const url = normalizeSourceUrl(item.url || item.link || item.href || "");
    if (url) return { title: title || url, url };
    return parseSourceLine(title);
  }
  return null;
}

function normalizeSourceItems(value) {
  if (!value) return [];
  if (Array.isArray(value)) {
    return value.map(normalizeSourceItem).filter(Boolean);
  }
  if (typeof value === "string") {
    return value
      .split(/\r?\n/)
      .map(normalizeSourceItem)
      .filter(Boolean);
  }
  return [normalizeSourceItem(value)].filter(Boolean);
}

function extractSourceBlockFromContent(content) {
  const text = String(content || "");
  const sourceHeadingPattern = /(?:^|\n)\s*(?:#{1,6}\s*)?(?:数据来源|参考来源|资料来源|来源)\s*[:：]?\s*\n?/g;
  let match = null;
  let current = null;
  while ((current = sourceHeadingPattern.exec(text))) {
    match = current;
  }
  if (!match) {
    return { content: text, sources: [] };
  }

  const before = text.slice(0, match.index).trimEnd();
  const rawSourceBlock = text.slice(match.index + match[0].length).trim();
  const sources = rawSourceBlock
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !/^[-*•\d.、\s]*$/.test(line))
    .map(normalizeSourceItem)
    .filter(Boolean);

  return {
    content: before,
    sources,
  };
}

function assistantSourceItems(message) {
  const structuredSources = [
    ...normalizeSourceItems(message?.sourceInfo),
    ...normalizeSourceItems(message?.sources),
    ...normalizeSourceItems(message?.references),
  ];
  if (structuredSources.length) return structuredSources;
  return extractSourceBlockFromContent(message?.content).sources;
}

function assistantDisplayContent(message) {
  const extracted = extractSourceBlockFromContent(message?.content);
  if (extracted.sources.length || extracted.content !== String(message?.content || "")) {
    return extracted.content;
  }
  return String(message?.content || "");
}

function toggleMessageSources(message, index) {
  const key = messageSourceKey(message, index);
  activeSourceMessageKey.value = activeSourceMessageKey.value === key ? null : key;
}

function renderAssistantMessage(content) {
  return markdown.render(String(content || ""));
}

function assistantContextTags(message) {
  const tags = [];
  if (message?.usedWeatherContext) {
    tags.push({
      key: "weather",
      label: "已结合天气信息",
      tone: "weather",
    });
  }
  if (message?.usedAttractionStatusContext) {
    tags.push({
      key: "attraction-status",
      label: "已参考开放状态",
      tone: "status",
    });
  }
  return tags;
}

function resizeComposer() {
  const element = composerInput.value;
  if (!element) {
    return;
  }
  const minHeight = 32;
  const maxHeight = 176;
  element.style.height = "auto";
  const nextHeight = Math.min(Math.max(element.scrollHeight, minHeight), maxHeight);
  element.style.height = `${nextHeight}px`;
  element.style.overflowY = element.scrollHeight > maxHeight ? "auto" : "hidden";
  isComposerExpanded.value = nextHeight > 44;
}

function handleComposerKeydown(event) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    handleComposerSubmit();
  }
}

function handleComposerSubmit() {
  if (props.isStreaming) {
    emit("stop-streaming");
    return;
  }
  emit("send-message");
}

function scrollToBottom() {
  if (!messageScroller.value) {
    return;
  }
  messageScroller.value.scrollTop = messageScroller.value.scrollHeight;
}

function scheduleScrollToBottom() {
  if (scrollFrameId) {
    return;
  }
  scrollFrameId = window.requestAnimationFrame(() => {
    scrollFrameId = 0;
    scrollToBottom();
  });
}

function handleDocumentPointerDown(event) {
  const root = weatherPanelRoot.value;
  if (props.isWeatherPanelOpen && root && !root.contains(event.target)) {
    emit("close-weather-panel");
  }
  if (!event.target?.closest?.(".message-source-menu")) {
    activeSourceMessageKey.value = null;
  }
}

async function focusComposer() {
  await nextTick();
  composerInput.value?.focus();
  resizeComposer();
}

function isLastAssistantMessage(index) {
  for (let i = props.messages.length - 1; i >= 0; i--) {
    if (props.messages[i].role === 'assistant') {
      return i === index;
    }
  }
  return false;
}

async function handleCopyMessage(message) {
  try {
    const content = assistantDisplayContent(message);
    await navigator.clipboard.writeText(content);
    emit("copy-message", { success: true });
  } catch (error) {
    emit("copy-message", { success: false, error: error.message });
  }
}

function handleRegenerateMessage(index) {
  // 找到这条助手消息对应的用户消息
  let userMessageIndex = -1;
  for (let i = index - 1; i >= 0; i--) {
    if (props.messages[i].role === 'user') {
      userMessageIndex = i;
      break;
    }
  }

  if (userMessageIndex >= 0) {
    const userMessage = props.messages[userMessageIndex];
    emit("regenerate-message", {
      userMessage: userMessage.content,
      assistantIndex: index
    });
  }
}

watch(
  () => props.chatInput,
  () => nextTick(() => resizeComposer()),
);

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    scrollToBottom();
  },
);

onMounted(() => {
  resizeComposer();
  document.addEventListener("mousedown", handleDocumentPointerDown);
});

onBeforeUnmount(() => {
  if (scrollFrameId) {
    window.cancelAnimationFrame(scrollFrameId);
  }
  document.removeEventListener("mousedown", handleDocumentPointerDown);
});

defineExpose({
  focusComposer,
  modelMenuRoot,
  scheduleScrollToBottom,
  scrollToBottom,
  weatherPanelRoot,
});
</script>
