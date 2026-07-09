/**
 * LLM 提供商配置常量
 * 定义支持的提供商及其元数据
 */

/**
 * 提供商元数据配置
 * @type {Object.<string, {summary: string, baseUrlPlaceholder: string, requiresApiKey: boolean, requiresBaseUrl: boolean}>}
 */
export const PROVIDER_META = {
  siliconflow: {
    summary: 'SiliconFlow 云端 API',
    baseUrlPlaceholder: 'https://api.siliconflow.cn/v1',
    requiresApiKey: true,
    requiresBaseUrl: false
  },
  ollama: {
    summary: 'Ollama 本地部署',
    baseUrlPlaceholder: 'http://localhost:11434',
    requiresApiKey: false,
    requiresBaseUrl: true
  }
}

/**
 * 可用的提供商列表
 * 从 PROVIDER_META 派生
 * @type {string[]}
 */
export const PROVIDER_OPTIONS = Object.keys(PROVIDER_META)
