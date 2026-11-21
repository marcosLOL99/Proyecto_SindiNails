# CLAUDE.md — frontend (SindiNails)

Complementa el `CLAUDE.md` de la raíz (peligros, comandos, skills, convenciones y Git siguen allí).

## Rediseño visual

- La web debe quedar **totalmente distinta a la actual**. No conserves el aspecto existente por inercia.
- Antes de diseñar una página o una sección, consulta `ui-ux-pro-max` (sistema de diseño, paleta, tipografía, patrones) y propón la dirección visual al usuario **antes** de implementarla.
- Los colores y la tipografía van como **tokens** (variables CSS en Tailwind/shadcn), nunca como hex sueltos en los componentes.
- Requisitos mínimos: accesibilidad (contraste 4.5:1, foco visible, navegación con teclado, labels reales), diseño mobile-first (la mayoría de clientas reservará desde el móvil) y `prefers-reduced-motion` en las animaciones.
- Separa el rediseño de la lógica: los tests prueban **comportamiento** (qué puede hacer el usuario), no markup ni clases CSS. Así sobreviven al rediseño.
- Script de búsqueda: `python "$HOME/.claude/skills/ui-ux-pro-max/scripts/search.py" "<consulta>" --design-system`. Python 3.12 está instalado; si no lo encuentra, la ruta es `%LOCALAPPDATA%\Programs\Python\Python312\python.exe`.

## Testing — Vitest + jsdom + @testing-library/react + user-event

- Configurado en `vite.config.js` (bloque `test`). El setup común está en `src/test/setup.js`: matchers de jest-dom, `cleanup()` y `localStorage.clear()` tras cada test. Los imports de Vitest son explícitos (`import { describe, it, expect } from "vitest"`), sin globals.
- La API se simula con MSW (`msw/node`, `setupServer`, rutas con `*/api/...`); ejemplo en `components/CitaForm.test.jsx`. No se hacen mocks de `apiClient` ni de axios.
- Los tests del frontend se ejecutan con `TZ=Europe/Madrid` (bloque `test.env` de `vite.config.js`), así las fechas no dependen del equipo.
- Usa los providers reales (`AuthProvider`, `createMemoryRouter`) en vez de hacer mocks de hooks como `useAuth`.
- Comprueba que cada test nuevo puede fallar: rompe temporalmente el código, mira el test en rojo y restaura.
- Las skills `front-end-testing` y `react-testing` recomiendan Vitest Browser Mode. **En este proyecto se usa jsdom**: sigue sus patrones de la variante Testing Library (en `resources/*-legacy.md`) y su filosofía (consultas por rol/label, interacciones de usuario reales, nada de detalles de implementación).
- Playwright E2E queda para más adelante, solo para los flujos críticos (registro → login → reservar cita; admin gestiona una cita).
- Los tests van junto al componente: `Componente.test.jsx`.
