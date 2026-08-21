import { createContext, useContext, useEffect, useState } from 'react'
import {
  clearToken,
  getFeatures,
  getMe,
  getToken,
  login as apiLogin,
  register as apiRegister,
  setToken,
  updateFeatures as apiUpdateFeatures,
} from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(getToken())
  const [features, setFeatures] = useState({})
  const [isAdmin, setIsAdmin] = useState(false)

  useEffect(() => {
    getFeatures()
      .then(setFeatures)
      .catch(() => setFeatures({}))
  }, [token])

  useEffect(() => {
    getMe()
      .then((me) => setIsAdmin(me.isAdmin))
      .catch(() => setIsAdmin(false))
  }, [token])

  async function login(username, password) {
    const { token } = await apiLogin(username, password)
    setToken(token)
    setTokenState(token)
  }

  async function register(username, password) {
    const { token } = await apiRegister(username, password)
    setToken(token)
    setTokenState(token)
  }

  function logout() {
    clearToken()
    setTokenState(null)
  }

  async function setFeatureOverride(overrides) {
    const updated = await apiUpdateFeatures(overrides)
    setFeatures(updated)
    return updated
  }

  return (
    <AuthContext.Provider value={{ token, login, register, logout, features, setFeatureOverride, isAdmin }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
