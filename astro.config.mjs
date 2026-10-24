import { defineConfig } from 'astro/config';

export default defineConfig({
  output: 'static',
  build: { format: 'file' },   // Capacitor needs plain .html files
  vite: {
    server: { host: true },    // lets your phone hit `astro dev` via LAN
  },
});
