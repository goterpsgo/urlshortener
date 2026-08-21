const TOKEN_KEY = 'urlshortener.token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

async function request(path, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(path, { ...options, headers })

  if (!response.ok) {
    const message = await response.text().catch(() => '')
    throw new Error(message || `Request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

export function register(username, password) {
  return request('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export function login(username, password) {
  return request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export function createLink(originalUrl) {
  return request('/api/links', { method: 'POST', body: JSON.stringify({ originalUrl }) })
}

export function listLinks() {
  return request('/api/links')
}

export function updateLink(id, originalUrl) {
  return request(`/api/links/${id}`, { method: 'PUT', body: JSON.stringify({ originalUrl }) })
}
