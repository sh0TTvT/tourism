// src/utils/api.js
import { storage } from './storage'
import { STORAGE_KEYS } from '@/constants'

const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:8080'

export async function apiRequest(endpoint, options = {}) {
  const token = storage.get(STORAGE_KEYS.token)

  const headers = {
    'Content-Type': 'application/json',
    ...options.headers
  }

  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const config = {
    ...options,
    headers
  }

  if (options.body && typeof options.body === 'object') {
    config.body = JSON.stringify(options.body)
  }

  try {
    const response = await fetch(`${API_BASE}${endpoint}`, config)

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: response.statusText }))
      throw new Error(error.message || `HTTP ${response.status}`)
    }

    const contentType = response.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      return await response.json()
    }

    return await response.text()
  } catch (error) {
    console.error(`API request failed: ${endpoint}`, error)
    throw error
  }
}
