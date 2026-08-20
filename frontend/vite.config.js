import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  base: '/app/',
  plugins: [react()],
  server: {
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    },
  },
})
