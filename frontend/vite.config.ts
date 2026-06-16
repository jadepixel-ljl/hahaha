import { defineConfig, type PluginOption } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue() as PluginOption],
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/uploads': 'http://127.0.0.1:8080',
    },
  },
});
