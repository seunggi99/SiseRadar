import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // .env lives at the repo root; load VITE_* vars from there.
  // (Vite only exposes VITE_-prefixed vars to the client — backend secrets stay server-side.)
  envDir: '..',
  server: {
    port: 5173,
    strictPort: true,
  },
});
