import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// `npm run build` writes straight into Spring Boot's static folder, so one jar serves API + UI.
export default defineConfig({
  plugins: [react()],
  define: { global: 'window' },                       // sockjs-client expects `global`
  build: { outDir: '../src/main/resources/static', emptyOutDir: true },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': { target: 'http://localhost:8080', ws: true },
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
    },
  },
});
