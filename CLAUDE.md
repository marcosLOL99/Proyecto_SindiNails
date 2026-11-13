# CLAUDE.md — SindiNails

Web de reservas para un salón de uñas: parte pública (inicio, servicios, contacto), área de cliente (reservar y cancelar citas) y panel de administración (citas, servicios, trabajadores, clientes, turnos y ausencias).

**Estado:** el proyecto está en pleno **refactor completo y rediseño visual total**. Se trabaja **paso a paso**: cada paso es pequeño, se prueba y lo valida el usuario antes de pasar al siguiente.

Comunicación con el usuario: **en español**.

---

## Stack

| Parte | Tecnología |
|---|---|
| Backend | Spring Boot 3.5, Java 21, Spring Security + JWT (jjwt 0.11.5), JPA/Hibernate, MySQL 8, Lombok, Maven (`mvnw`) |
| Frontend | React 19, Vite 7, **JavaScript (.jsx, sin TypeScript)**, Tailwind 3, shadcn/ui (Radix), react-hook-form + zod, axios, react-router 7, date-fns, framer-motion, recharts, sonner |
| Infra | `docker-compose.yml` (db + backend + frontend con nginx en el puerto 80) |

**Entorno:** de momento el proyecto **solo se usa en local**. No hay producción. El despliegue anterior en Railway está abandonado, y dónde publicarlo se decidirá al terminar el refactor. No añadas configuración específica de ningún proveedor de hosting. Todo lo que dependa del entorno (URLs, credenciales, orígenes CORS) va por variables de entorno.

## Estructura

```
backend-springboot/src/main/java/com/SindiNails/backend/
  controller/  dto/  model/  repository/  service/  security/  exception/
backend-springboot/init-db/dump.sql      # datos iniciales que carga el MySQL de docker
frontend-react/src/
  pages/{publico,cliente,admin}/  components/  components/ui/ (shadcn)
  router/ (AppRouter, ProtectedRoute, api.js = cliente axios)  context/ (AuthContext, AdminRoute)
  services/  lib/utils.js
```

Alias `@/` → `frontend-react/src/`.

## Comandos

Configuración: copia `.env.example` a `.env` en la raíz (`.env` está en `.gitignore`). Lo leen tanto docker compose como el backend (`spring.config.import`).

Backend (desde `backend-springboot/`, en Windows `mvnw.cmd`):
- Arrancar en local: `docker compose up -d db` (MySQL en `localhost:3307`) y después `./mvnw spring-boot:run` (puerto 8080)
- Tests: `./mvnw test` (requiere Docker Desktop abierto: cada ejecución levanta un MySQL desechable con Testcontainers)
- Un test: `./mvnw test -Dtest=NombreDelTest`

Frontend (desde `frontend-react/`, Node.js 26 LTS):
- `npm run dev` (http://localhost:5173; Vite redirige `/api` a `localhost:8080`) · `npm run build` · `npm run lint`
- Si la terminal no encuentra `node`/`npm`, la ruta es `C:\Program Files\nodejs`.
- npm 11 no ejecuta scripts de instalación sin aprobar (el aviso de `esbuild` es normal y no afecta).
- Tests: `npm test` (modo watch) / `npm run test:run` (una vez) / `npx vitest run ruta/al/archivo.test.jsx`

Todo junto: `docker compose up --build` → http://localhost

---

## ⚠️ Peligros conocidos (no ignorar)

1. ~~Base de datos de Railway por defecto~~ y ~~secretos en `docker-compose.yml`~~: **resuelto** en la Fase 0 (MySQL local por defecto, secretos en `.env`, JWT nuevo). El secreto JWT antiguo sigue en el historial de git: no reutilizarlo nunca.
2. **El esquema de `init-db/dump.sql` no coincide con las entidades.** Al arrancar, Hibernate (`ddl-auto=update`) intenta cambiar `tipo_servicio.id` y `servicio_local.tipo_servicio_id` a `bigint`, falla por la clave foránea `fk_tipo_servicio` y deja un WARN. Pendiente: alinear el dump con las entidades (o pasar a migraciones con Flyway) y quitar `ddl-auto=update`.
3. **Sin token, el backend responde 403 en vez de 401**, y **un login con credenciales incorrectas también devuelve 403**. Pero el interceptor de `router/api.js` solo cierra la sesión con 401, así que un token caducado no devuelve al usuario al login. Es un comportamiento actual: caracterízalo antes de corregirlo.
4. ~~CORS ligado a Railway y frontend local sin acceso al backend~~: **resuelto** (orígenes en `CORS_ALLOWED_ORIGINS` y proxy `/api` en Vite). ~~`authService.js` con URL a pelo~~: era código muerto, eliminado.
5. El token JWT se guarda en `localStorage`.
6. No hay manejo global de errores en el backend (no existe `@ControllerAdvice`). Las reglas de negocio lanzan `IllegalStateException`, que llega al cliente como **500 sin el mensaje**, así que el frontend muestra un error genérico en vez de, por ejemplo, "La cita ya está cancelada.".
6b. **Probable desfase horario en el panel de administración:** `components/CitaForm.jsx` envía `fechaHoraInicio` con `toISOString()` (UTC, `...Z`), mientras que la reserva de clientas envía la hora local. Si el backend descarta la zona, las citas creadas o movidas desde el panel se guardan con una hora de diferencia y, con las reglas de turno de la Fase 1, pueden rechazarse. Hay que caracterizarlo y verificarlo antes de corregirlo.
7. **`AuthContext` registra un interceptor de axios nuevo en cada render.** `logout` se crea de nuevo en cada render y está en las dependencias del `useEffect` que llama a `setupInterceptors`, así que los interceptores se acumulan y nunca se eliminan. Hay que caracterizarlo antes de corregirlo.
8. **Lint:** `npm run lint` tiene 24 problemas que ya existían; se irán corrigiendo durante el refactor. El código nuevo o modificado debe pasar el lint.
9. Detalles menores: `RopositorioPeriodoNoDisp` (errata); `tailwind.config.cjs` usa `export default` (sintaxis ESM) con extensión `.cjs`, y `components.json` apunta a `tailwind.config.js`; `README.md` está en UTF-16.

No te conectes a la base de datos antigua de Railway ni a ningún servicio remoto. Todo se ejecuta contra el MySQL de docker en local.

### Reglas de negocio de las citas (`CitaService`)

Fijadas por tests permanentes en `CitaServiceTest` (reloj congelado con `RelojFijo`). "Ahora" es la hora del salón: el `Clock` de `ClockConfig` usa `app.zona-horaria` (`Europe/Madrid`), no la zona del sistema (en docker la JVM está en UTC). Citas y turnos se guardan como hora local del salón. Los bugs S1–S7 encontrados al caracterizar en la Fase 1 se corrigieron así:

- **Reservar o mover una cita** (también la administración): no en el pasado; trabajadora activa; la cita entera dentro de uno de sus turnos de ese día; sin pisar ausencias de la trabajadora ni bloqueos globales; sin solaparse con otra cita no cancelada. No hace falta empezar en un cuarto de hora. Una cita cancelada no se puede mover.
- **Huecos libres**: cada 15 minutos dentro de los turnos, ordenados por hora; nunca huecos que ya han pasado; ninguno para una trabajadora inactiva; con turnos que acaban cerca de medianoche, solo huecos que terminan ese mismo día (antes el cálculo daba la vuelta a las 00:00 y no terminaba nunca).
- **Cancelar** (clienta): solo la dueña, con 12 horas o más de antelación (justo 12 h vale); no una cita ya cancelada ni una que ya ha pasado (cada caso con su mensaje).
- **Resumen semanal del panel**: últimos 7 días (desde hace 6 días a las 00:00), sin contar las canceladas.

Lo que todavía no tiene test permanente sigue en `CitaServiceCharacterisationTest` (huecos con citas y bloqueos, días no disponibles, borrar, listados): se sustituirá al refactorizar `CitaService`.

Notas de diseño para el refactor: `getDiasNoDisponibles` llama a `getHorariosDisponibles` 42 veces, con varias consultas en cada llamada; `createCita` y `updateCita` repiten la carga de servicio y trabajadora y el cálculo del fin.

---

## Skills y cuándo usar cada una

Están en `~/.claude/skills/`. Este es el orden de trabajo:

| Situación | Skill |
|---|---|
| Vas a cambiar código que **no tiene tests** (casi todo, al principio) | `characterisation-tests` **primero**: fija lo que el código hace hoy, aunque sea un bug |
| Reestructurar sin cambiar comportamiento, con los tests en verde | `refactoring` |
| Añadir o cambiar comportamiento, o corregir un bug | `tdd` (RED → GREEN → REFACTOR) |
| Diseñar o rehacer la interfaz de un módulo o servicio | `codebase-design` |
| Cualquier trabajo visual: páginas, componentes, colores, tipografía, accesibilidad | `ui-ux-pro-max` |
| Tests del frontend | `front-end-testing` + `react-testing` |

Herramientas integradas: `/code-review` antes de cerrar cada fase y `/security-review` en todo lo que toque autenticación, JWT, roles o `SecurityConfig`.

### Reglas que las skills dan por hecho

- **Primero un baseline en verde.** No se refactoriza código que no tenga tests. Si no los tiene, primero se escriben tests de caracterización.
- **Los bugs que aparezcan al caracterizar no se arreglan sobre la marcha.** Se fija el comportamiento actual en un test, se anota el bug y se le comenta al usuario. Su arreglo es un cambio de comportamiento aparte, con `tdd`.
- **Refactor y cambios de comportamiento nunca van en el mismo paso** (ni en el mismo commit).
- **Un cambio lógico por paso.** Tras cada paso se ejecutan los tests afectados. Si fallan, se revierte el paso en vez de "arreglar hacia delante".
- **Evaluación del refactor:** antes de editar, lista los candidatos con `Critical:` / `High:` / `Nice:` / `Skip:` y termina con `Decision:` (formato de la skill `refactoring`).
- **DRY se aplica al conocimiento, no al código que se parece.** Dos reglas de negocio que hoy coinciden (por ejemplo, la duración de dos servicios) no se fusionan.
- **Nada especulativo:** no añadas validaciones, abstracciones ni funcionalidades que ningún test o requisito pida.
- **Mutation testing al cierre de cada fase**, no tras cada paso: PIT en el backend y StrykerJS en el frontend. Se instalan al cerrar la primera fase. Mientras no existan, indica `N/A` y qué otra prueba lo respalda.

### Rediseño visual

- La web debe quedar **totalmente distinta a la actual**. No conserves el aspecto existente por inercia.
- Antes de diseñar una página o una sección, consulta `ui-ux-pro-max` (sistema de diseño, paleta, tipografía, patrones) y propón la dirección visual al usuario **antes** de implementarla.
- Los colores y la tipografía van como **tokens** (variables CSS en Tailwind/shadcn), nunca como hex sueltos en los componentes.
- Requisitos mínimos: accesibilidad (contraste 4.5:1, foco visible, navegación con teclado, labels reales), diseño mobile-first (la mayoría de clientas reservará desde el móvil) y `prefers-reduced-motion` en las animaciones.
- Separa el rediseño de la lógica: los tests prueban **comportamiento** (qué puede hacer el usuario), no markup ni clases CSS. Así sobreviven al rediseño.
- Script de búsqueda: `python "$HOME/.claude/skills/ui-ux-pro-max/scripts/search.py" "<consulta>" --design-system`. Python 3.12 está instalado; si no lo encuentra, la ruta es `%LOCALAPPDATA%\Programs\Python\Python312\python.exe`.

---

## Testing

**Backend — JUnit 5 + Spring Boot Test + Testcontainers (MySQL 8)**
- Todo test de Spring lleva `@ActiveProfiles("test")` + `@Import(TestcontainersConfiguration.class)`. Así usa un MySQL 8 desechable (`@ServiceConnection`) y los valores fijos de `src/test/resources/application-test.properties` (incluido un secreto JWT solo para tests). No depende del `.env`.
- Los tests de integración nunca usan el MySQL de desarrollo ni ninguna base de datos remota. La base de datos del contenedor empieza vacía (sin `dump.sql`): cada test crea sus propios datos.
- `testcontainers.version` está fijada a 1.21.4 en el `pom.xml`, porque la 1.21.3 que trae Spring Boot no funciona con Docker Engine 29. No la quites al actualizar Spring Boot salvo que este ya traiga una versión igual o superior.
- Prioridad de caracterización: `CitaService` (disponibilidad de horarios, días no disponibles, solapes con turnos y ausencias, cancelación), autenticación/JWT y permisos por rol (`/api/admin/**` solo para ADMIN).
- Para endpoints: `@SpringBootTest` + MockMvc, comprobando estado HTTP, cuerpo JSON y seguridad (401/403).

**Frontend — Vitest + jsdom + @testing-library/react + user-event**
- Configurado en `vite.config.js` (bloque `test`). El setup común está en `src/test/setup.js`: matchers de jest-dom, `cleanup()` y `localStorage.clear()` tras cada test. Los imports de Vitest son explícitos (`import { describe, it, expect } from "vitest"`), sin globals.
- Para simular la API se usará MSW, que se instalará con el primer test que lo necesite. No se hacen mocks de `apiClient` ni de axios.
- Usa los providers reales (`AuthProvider`, `createMemoryRouter`) en vez de hacer mocks de hooks como `useAuth`.
- Comprueba que cada test nuevo puede fallar: rompe temporalmente el código, mira el test en rojo y restaura.
- Las skills `front-end-testing` y `react-testing` recomiendan Vitest Browser Mode. **En este proyecto se usa jsdom**: sigue sus patrones de la variante Testing Library (en `resources/*-legacy.md`) y su filosofía (consultas por rol/label, interacciones de usuario reales, nada de detalles de implementación).
- Playwright E2E queda para más adelante, solo para los flujos críticos (registro → login → reservar cita; admin gestiona una cita).
- Los tests van junto al componente: `Componente.test.jsx`.

---

## Convenciones

- **Nombres del dominio en español**, coherentes con lo que ya existe: `Cita`, `Trabajador`, `Turno`, `Servicio`, `PeriodoNoDisponible`…
- Backend por capas: controller (solo HTTP) → service (reglas de negocio) → repository. Las entidades JPA nunca salen por la API: siempre DTOs. La validación de entrada va con `jakarta.validation` en los DTOs.
- Frontend: todas las llamadas HTTP pasan por `apiClient` (`router/api.js`) o por servicios que lo usen. Nada de URLs a pelo ni instancias sueltas de axios. Formularios con react-hook-form + zod.
- Componentes de `components/ui/` (shadcn): se generan con la CLI de shadcn y no se reescriben a mano sin motivo.

## Git

- **No hagas commits sin aprobación explícita del usuario.**
- Trabaja en ramas por fase (por ejemplo `refactor/fase-0-seguridad`), nunca directamente en `main`.
- Mensajes: `test: …`, `refactor: …`, `feat: …`, `fix: …`, `style: …`, `chore: …`. Un commit nunca mezcla refactor con funcionalidad.
