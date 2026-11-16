import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: {
    rollupOptions: {
      output: {
        // Bibliothèques dans des fichiers séparés (mis en cache par le navigateur)
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom', '@tanstack/react-query'],
          mui: ['@mui/material', '@emotion/react', '@emotion/styled'],
          icones: ['@mui/icons-material'],
        },
      },
    },
  },
});
