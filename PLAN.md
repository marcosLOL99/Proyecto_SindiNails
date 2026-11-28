# PLAN.md — Hoja de ruta de SindiNails

Plan de fases del refactor completo y el rediseño visual. Es la guía para retomar el trabajo desde cualquier ordenador: dice qué está hecho, en qué fase estamos y cuál es el siguiente paso.

Las reglas de trabajo (skills, tests, commits) están en `CLAUDE.md`. Este archivo solo dice **qué** se hace y **en qué orden**.

---

## Cómo retomar el trabajo (al empezar cada sesión)

1. `git fetch --all` y `git status`. Comprueba que la rama local **no va por detrás** de `origin`. Si va por detrás, actualízala (`git merge --ff-only origin/<rama>`) **antes de tocar nada**.
2. Mira la tabla de "Dónde estamos": la fase en curso y su rama.
3. Cámbiate a la rama de la fase en curso (`git checkout <rama>`). Si la fase aún no tiene rama, créala desde `main` actualizado.
4. En la sección de la fase, el siguiente paso es la primera casilla sin marcar `[ ]`.
5. Al terminar un paso validado por el usuario, márcalo `[x]` **en el mismo commit** que el trabajo (o en un `docs:` justo después).
6. Al cerrar una fase, márcala ✅ en la tabla y **borra su sección entera, con todos sus pasos**. El archivo se acorta y solo quedan las fases pendientes. Lo hecho queda en git (commit `Merge Fase N: …`).

## Dónde estamos

| Fase | Tema | Rama | Estado |
|---|---|---|---|
| 0 | Entorno local, configuración y tests | `refactor/fase-0-entorno-local` | ✅ Fusionada |
| 1 | Caracterización y reglas de `CitaService` | `refactor/fase-1-caracterizacion-backend` | ✅ Fusionada |
| 2 | API y seguridad | `refactor/fase-2-api-seguridad` | ✅ Fusionada |
| **3** | **Resto del backend: caracterizar y corregir pendientes** | `refactor/fase-3-backend-pendientes` | 🔄 **En curso** |
| 4 | Refactor del backend y base de datos con Flyway | `refactor/fase-4-refactor-backend-bd` | ⏳ Pendiente |
| 5 | Frontend: lógica y tests (antes del rediseño) | `refactor/fase-5-frontend-logica` | ⏳ Pendiente |
| 6 | Sistema de diseño (dirección visual y tokens) | `feat/fase-6-sistema-diseno` | ⏳ Pendiente |
| 7 | Rediseño de la parte pública y del área de clienta | `feat/fase-7-rediseno-publico` | ⏳ Pendiente |
| 8 | Rediseño del panel de administración | `feat/fase-8-rediseno-admin` | ⏳ Pendiente |
| 9 | Calidad final: E2E, accesibilidad y rendimiento | `refactor/fase-9-calidad` | ⏳ Pendiente |
| 10 | Preparar la publicación | `chore/fase-10-publicacion` | ⏳ Pendiente |

**Por qué este orden:** primero el backend queda fijado por tests y sin bugs conocidos (3), después se reestructura con esa red (4). Luego se hace lo mismo con la lógica del frontend (5), para que el rediseño (6–8) solo cambie la presentación y los tests de comportamiento sigan en verde. Al final vienen la calidad de extremo a extremo (9) y la publicación (10).

## Cierre de cada fase (siempre igual)

- [ ] Todos los tests en verde (`./mvnw test` y/o `npm run test:run`) y `npm run lint` sin problemas nuevos.
- [ ] Mutation testing sobre el código cambiado en la fase (PIT y/o StrykerJS). Los mutantes equivalentes se anotan.
- [ ] `/code-review` y, si se tocó autenticación, roles o `SecurityConfig`, `/security-review`.
- [ ] Actualizar "Peligros conocidos" en `CLAUDE.md`.
- [ ] En este `PLAN.md`: la fase pasa a ✅ en la tabla y se borra su sección con todos sus pasos.
- [ ] Merge a `main` con un commit `Merge Fase N: …` que resuma la fase (con la aprobación del usuario).

---

## Fase 3 — Resto del backend: caracterizar y corregir pendientes

**Objetivo:** que todo el backend tenga tests y ningún bug conocido antes de reestructurarlo. Las Fases 1 y 2 cubrieron `CitaService`, la autenticación, los permisos, los errores y el catálogo. Quedan sin tests de comportamiento la gestión de clientas, turnos y ausencias o bloqueos, los listados de citas y los pendientes del punto 10 de `CLAUDE.md`.

Cada bug sigue el ciclo: caracterizar (test que fija lo que pasa hoy) → contárselo al usuario → corregirlo con `tdd` en un paso aparte.

- [x] 3.1 Caracterizar la gestión de clientas desde la administración (`UsuarioService` y `/api/admin/clientes`: listar, editar, borrar), incluido si editar deja el rol a `null`.
- [ ] 3.2 Corregir lo encontrado en 3.1 (decidido con el usuario; un paso `tdd` cada uno):
  - [x] C1: editar una clienta desde la administración nunca cambia su rol (ni a `null` ni a ADMIN): el rol del cuerpo se ignora.
  - [x] C2: `/api/admin/clientes/{id}` solo trabaja con clientas: editar o borrar una administradora (incluida una misma) responde 404.
  - [ ] C3: editar una clienta exige lo mismo que el registro: todos los campos obligatorios y un email válido.
  - [ ] C4: borrar una clienta con citas responde 409 con mensaje y no borra nada (sus citas son el historial del salón).
- [ ] 3.3 Caracterizar los turnos (`TurnoService` y `/api/admin/turnos`: crear, crear en lote, editar, borrar, listar por trabajadora; turnos que se solapan o con fin antes del inicio).
- [ ] 3.4 Caracterizar las ausencias y los bloqueos (`PeriodoNoDisponibleService` y `/api/admin/bloqueos`: crear, listar por trabajadora, listar globales, borrar; periodos con fin antes del inicio).
- [ ] 3.5 Corregir los bugs que salgan en 3.3 y 3.4 (un paso `tdd` por bug, después de decidirlo con el usuario).
- [ ] 3.6 Caracterizar el JSON de los listados de citas: `/api/citas/mis-citas`, `/api/admin/citas`, `/api/admin/stats/citas-semanales`, `/api/citas/disponibilidad`, `/api/citas/dias-no-disponibles`.
- [ ] 3.7 Corregir: un email duplicado por una carrera (o puesto por la administración) da 409 con mensaje, no 500 (`DataIntegrityViolationException` en `ManejadorDeErrores`).
- [ ] 3.8 Corregir: los 404 llegan con `{"message": "..."}` como el resto de errores.
- [ ] 3.9 Corregir: dos reservas simultáneas del mismo hueco no quedan confirmadas las dos (test con dos reservas en paralelo; bloqueo por trabajadora o restricción en la base de datos).
- [ ] 3.10 Corregir: `toString()`/`hashCode()` de las entidades no entran en recursión (sustituir `@Data` de Lombok en las entidades JPA).
- [ ] 3.11 Cierre de fase.

## Fase 4 — Refactor del backend y base de datos con Flyway

**Objetivo:** reestructurar el backend sin cambiar el comportamiento (con la red de tests de la Fase 3) y tener un esquema de base de datos versionado. Antes de editar se hace la evaluación `Critical/High/Nice/Skip → Decision` de la skill `refactoring`.

- [ ] 4.1 `CitaService`: diseñar su interfaz con `codebase-design`. Una sola comprobación de "el hueco cabe" para lo que se ofrece y lo que se acepta. `getDiasNoDisponibles` sin 42 llamadas con varias consultas cada una. Quitar lo repetido entre `createCita` y `updateCita`.
- [ ] 4.2 Sustituir `CitaServiceCharacterisationTest` por tests de comportamiento permanentes y borrarlo. Lo mismo con el resto de `*CharacterisationTest` que ya estén cubiertos.
- [ ] 4.3 Revisar los controladores: `AdminController` (184 líneas, todos los recursos) ¿se divide por recurso? Controladores solo HTTP.
- [ ] 4.4 Erratas y nombres: `RopositorioPeriodoNoDisp` → `RepositorioPeriodoNoDisponible`.
- [ ] 4.5 Flyway: migración `V1` con el esquema que esperan las entidades, `ddl-auto=validate` y fuera `ddl-auto=update`.
- [ ] 4.6 Datos de ejemplo para desarrollo local (sustituyen a `init-db/dump.sql`), separados del esquema y sin contraseñas reales. Ajustar `docker-compose.yml` e `INSTALACION.md`.
- [ ] 4.7 Cierre de fase.

## Fase 5 — Frontend: lógica y tests (antes del rediseño)

**Objetivo:** fijar con tests lo que puede hacer cada usuaria y sanear la lógica, para que el rediseño solo toque la presentación. Hoy el frontend tiene solo 2 archivos de test.

- [ ] 5.1 Tests de autenticación con MSW y providers reales: login, registro, cerrar sesión, `ProtectedRoute` y `AdminRoute`.
- [ ] 5.2 Corregir: `AuthContext` registra un interceptor de axios nuevo en cada render (peligro 7). Primero un test que lo detecte.
- [ ] 5.3 Corregir: con un token caducado, el interceptor 401 echa a la clienta a `/login` también desde las páginas públicas (decidir con el usuario qué debe pasar).
- [ ] 5.4 Tests del flujo de la clienta: ver servicios, reservar (días no disponibles, huecos, confirmar, error 409 con mensaje), ver y cancelar sus citas, editar el perfil.
- [ ] 5.5 Tests del panel de administración: citas, servicios, trabajadoras, turnos, ausencias y clientas (lo principal de cada una).
- [ ] 5.6 Refactor: todas las llamadas HTTP en `services/` sobre `apiClient`; formularios con react-hook-form + zod; lógica de las páginas en hooks, separada del markup.
- [ ] 5.7 `npm run lint` sin problemas (hoy, 24).
- [ ] 5.8 Cierre de fase (StrykerJS).

## Fase 6 — Sistema de diseño

**Objetivo:** una identidad visual nueva y totalmente distinta de la actual, definida como tokens antes de rediseñar páginas. Reglas en `frontend-react/CLAUDE.md`.

- [ ] 6.1 Investigar con `ui-ux-pro-max` (estilo, paleta, tipografía, patrones para un salón de uñas con reservas desde el móvil).
- [ ] 6.2 Proponer al usuario la dirección visual (2–3 opciones con muestras) y elegir una. **No se implementa nada antes.**
- [ ] 6.3 Tokens: colores (claro y, si se decide, oscuro), tipografía, radios, sombras y espaciado como variables CSS en Tailwind y shadcn. Contraste 4.5:1 comprobado.
- [ ] 6.4 Componentes base: los de `components/ui/` regenerados o tematizados con la CLI de shadcn; foco visible y `prefers-reduced-motion`.
- [ ] 6.5 Layout común: cabecera y navegación (móvil primero), pie de página, estados de carga, vacío y error.
- [ ] 6.6 Cierre de fase.

## Fase 7 — Rediseño de la parte pública y del área de clienta

**Objetivo:** las páginas que usan las clientas, rediseñadas con móvil primero. Los tests de la Fase 5 deben seguir en verde sin cambios (prueban comportamiento, no markup).

- [ ] 7.1 Inicio (`Home`, carrusel, testimonios, Instagram).
- [ ] 7.2 Servicios.
- [ ] 7.3 Contacto.
- [ ] 7.4 Login y registro.
- [ ] 7.5 Reservar cita (el flujo más importante: calendario, huecos, confirmación).
- [ ] 7.6 Área de clienta: mis citas, cancelar, editar el perfil.
- [ ] 7.7 Cierre de fase.

## Fase 8 — Rediseño del panel de administración

- [ ] 8.1 Layout del panel y resumen (dashboard con gráfica semanal).
- [ ] 8.2 Gestión de citas.
- [ ] 8.3 Servicios.
- [ ] 8.4 Trabajadoras.
- [ ] 8.5 Turnos y ausencias.
- [ ] 8.6 Clientas.
- [ ] 8.7 Cierre de fase.

## Fase 9 — Calidad final

- [ ] 9.1 Playwright E2E de los flujos críticos: registro → login → reservar cita; la administración gestiona una cita.
- [ ] 9.2 Auditoría de accesibilidad (axe/Lighthouse, teclado, lector de pantalla) y corrección de lo que salga.
- [ ] 9.3 Rendimiento: tamaño del bundle, carga diferida de rutas, imágenes.
- [ ] 9.4 Detalles menores: `README.md` a UTF-8 y actualizado, `tailwind.config.cjs`/`components.json` coherentes.
- [ ] 9.5 Cierre de fase.

## Fase 10 — Preparar la publicación

Todo se decide con el usuario. Hasta aquí el proyecto solo se usa en local.

- [ ] 10.1 Elegir dónde publicarlo (backend, base de datos y frontend).
- [ ] 10.2 Decidir dónde se guarda la sesión (peligro 5: token en `localStorage` frente a cookie `httpOnly`) y aplicarlo con `tdd` y `/security-review`.
- [ ] 10.3 Configuración de producción por variables de entorno: secretos nuevos, CORS, HTTPS, `docker-compose` o equivalente.
- [ ] 10.4 Copias de seguridad de la base de datos y registro de errores.
- [ ] 10.5 Primer despliegue y prueba de los flujos críticos en el entorno real.

---

## Notas e ideas fuera del plan

Funcionalidades nuevas que surjan (por ejemplo, recordatorios de citas) se apuntan aquí y **no** se hacen hasta decidirlo con el usuario: el plan es refactor y rediseño, no funcionalidad nueva.

- _(vacío)_
