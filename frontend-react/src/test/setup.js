// Configuración común de todos los tests (Vitest + jsdom).
import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach } from "vitest";

beforeEach(() => {
  // La sesión vive en localStorage: cada test empieza sin sesión.
  localStorage.clear();
});

afterEach(() => {
  // Sin globals de Vitest, Testing Library no desmonta solo: lo hacemos aquí.
  cleanup();
  localStorage.clear();
});
