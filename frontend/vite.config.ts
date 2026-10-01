import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Forward /api calls to the Spring Boot backend, so the browser sees one origin (no CORS setup needed in dev).
    // API_TARGET lets you point the dev server at a different backend, e.g. API_TARGET=http://localhost:8081
    proxy: {
      '/api': process.env.API_TARGET ?? 'http://localhost:8080',
    },
  },
})
