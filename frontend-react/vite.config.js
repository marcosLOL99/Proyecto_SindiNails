import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    // En desarrollo, /api se redirige al backend (igual que hace nginx en docker).
    // changeOrigin: false conserva la cabecera Host del navegador, así Spring ve la
    // petición como del mismo origen y CORS no interviene (aunque Vite cambie de puerto).
    // Ojo: la forma corta "/api": "http://..." activa changeOrigin: true por defecto.
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: false,
      },
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.js",
    // La zona del salón: así las fechas de los tests no dependen del equipo donde se ejecuten.
    env: { TZ: "Europe/Madrid" },
  },
});
