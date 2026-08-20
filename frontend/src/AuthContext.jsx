import { createContext, useContext, useState } from 'react'
import { clearToken, getToken, login as apiLogin, register as apiRegister, setToken } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setTokenState] = useState(getToken())

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

  return (
    <AuthContext.Provider value={{ token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
