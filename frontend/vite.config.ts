import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = process.env.API_PROXY_TARGET ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    // Бэкенд (Spring Boot) на :8080 — CORS настраивать не нужно. См. API.md, «Подключение в Vite».
    // В docker compose фронт и бэкенд в разных контейнерах: API_PROXY_TARGET=http://backend:8080.
    proxy: { '/api': backend, '/actuator': backend },
  },
});
