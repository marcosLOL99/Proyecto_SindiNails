// Configuración común de todos los tests (Vitest + jsdom).
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  // Sin globals de Vitest, Testing Library no desmonta solo: lo hacemos aquí.
  cleanup();
  // La sesión vive en localStorage: cada test empieza sin sesión.
  localStorage.clear();
});
