// src/utils/validators.js
export function validateEmail(email) {
  if (!email) {
    return '邮箱不能为空'
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(email)) {
    return '邮箱格式不正确'
  }
  return null
}

export function validateRequired(value, fieldName) {
  if (!value || (typeof value === 'string' && !value.trim())) {
    return `${fieldName}不能为空`
  }
  return null
}

export function validateUrl(url, fieldName) {
  if (!url) {
    return `${fieldName}不能为空`
  }
  try {
    new URL(url)
    return null
  } catch {
    return `${fieldName}格式不正确`
  }
}

export function parseJsonField(text, fieldName) {
  if (!text || !text.trim()) {
    return { value: null, error: null }
  }
  try {
    const value = JSON.parse(text)
    return { value, error: null }
  } catch (error) {
    return { value: null, error: `${fieldName}不是有效的 JSON 格式` }
  }
}
