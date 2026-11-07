import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: ['design-av-lekseradar-3001.v0.build'],
    // Бэкенд (Spring Boot) на :8080 — CORS настраивать не нужно. См. API.md, «Подключение в Vite».
    proxy: { '/api': 'http://localhost:8080', '/actuator': 'http://localhost:8080' },
  },
});
