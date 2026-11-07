import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: { allowedHosts: ['design-av-lekseradar-3001.v0.build'] },
});
