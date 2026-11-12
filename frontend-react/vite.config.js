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
    // En desarrollo, /api se redirige al backend (igual que hace nginx en docker),
    // así el navegador llama siempre al mismo origen y no interviene CORS.
    proxy: {
      "/api": {
        target: process.env.VITE_PROXY_TARGET || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
