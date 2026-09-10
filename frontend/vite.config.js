import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/oauth2': { target: process.env.GATEWAY_URL || 'http://localhost:18080', changeOrigin: true },
      '/login': { target: process.env.GATEWAY_URL || 'http://localhost:18080', changeOrigin: true },
      '/api': {
        target: process.env.GATEWAY_URL || 'http://localhost:18080',
        changeOrigin: true,
      },
    },
  },
})
