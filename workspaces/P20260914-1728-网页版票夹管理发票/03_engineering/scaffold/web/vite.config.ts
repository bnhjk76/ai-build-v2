import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      // 开发环境直连本地 api（8080）；生产由 Caddy 同源托管（engineering-plan §5）
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
