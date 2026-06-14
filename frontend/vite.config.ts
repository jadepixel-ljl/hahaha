import { defineConfig, type PluginOption } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue() as PluginOption],
});
