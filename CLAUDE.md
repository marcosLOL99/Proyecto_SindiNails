# CLAUDE.md — SindiNails

Web de reservas para un salón de uñas: parte pública (inicio, servicios, contacto), área de cliente (reservar y cancelar citas) y panel de administración (citas, servicios, trabajadores, clientes, turnos y ausencias).

**Estado:** el proyecto está en pleno **refactor completo y rediseño visual total**. Se trabaja **paso a paso**: cada paso es pequeño, se prueba y lo valida el usuario antes de pasar al siguiente.

Comunicación con el usuario: **en español**.

---

## Plan de fases → [`PLAN.md`](PLAN.md)

El plan de todas las fases, la fase en curso y el siguiente paso están en [`PLAN.md`](PLAN.md).

- **Al empezar cada sesión, antes de tocar nada:** `git fetch --all`, comprueba que la rama local no va por detrás de `origin` (si va, actualízala) y sigue la sección "Cómo retomar el trabajo" de `PLAN.md`.
- **Al terminar un paso:** márcalo `[x]` en su fase.
- **Al cerrar una fase:** márcala ✅ en la tabla "Dónde estamos" y **borra la sección entera de la fase con todos sus pasos**, para que el archivo se vaya acortando. Lo hecho queda en el historial de git y en el commit de merge de la fase.

---

## Stack

Frontend en JavaScript (`.jsx`), sin TypeScript.

**Entorno:** de momento el proyecto **solo se usa en local**. No hay producción. El despliegue anterior en Railway está abandonado, y dónde publicarlo se decidirá al terminar el refactor. No añadas configuración específica de ningún proveedor de hosting. Todo lo que dependa del entorno (URLs, credenciales, orígenes CORS) va por variables de entorno.

## Estructura

Alias `@/` → `frontend-react/src/`.

## Comandos

Equipo nuevo: sigue `INSTALACION.md` (programas, `.env`, skills y primer arranque).

Configuración: copia `.env.example` a `.env` en la raíz (`.env` está en `.gitignore`). Lo leen tanto docker compose como el backend (`spring.config.import`).

Backend (desde `backend-springboot/`, en Windows `mvnw.cmd`):
- Arrancar en local: `docker compose up -d db` (MySQL en `localhost:3307`) y después `./mvnw spring-boot:run` (puerto 8080)
- Tests: `./mvnw test` (requiere Docker Desktop abierto: cada ejecución levanta un MySQL desechable con Testcontainers)
- Un test: `./mvnw test -Dtest=NombreDelTest`
- Mutation testing (PIT, al cierre de fase): `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage -DtargetClasses=com.SindiNails.backend.service.CitaService` (varias clases separadas por comas). Informe en `target/pit-reports/index.html`. Tarda unos 3 minutos por los tests con Testcontainers.

Frontend (desde `frontend-react/`, Node.js 26 LTS):
- `npm run dev` (http://localhost:5173; Vite redirige `/api` a `localhost:8080`) · `npm run build` · `npm run lint`
- Si la terminal no encuentra `node`/`npm`, la ruta es `C:\Program Files\nodejs`.
- npm 11 no ejecuta scripts de instalación sin aprobar (el aviso de `esbuild` es normal y no afecta).
- Tests: `npm test` (modo watch) / `npm run test:run` (una vez) / `npx vitest run ruta/al/archivo.test.jsx`
- Mutation testing (StrykerJS, al cierre de fase): `npm run test:mutation -- --mutate src/components/CitaForm.jsx` (admite rango de líneas: `archivo.jsx:79-105`). Configuración en `stryker.config.json`; informe en `reports/mutation/mutation.html`. Sin `--mutate` muta todo `src/` salvo tests y `components/ui/` (tarda mucho).

Todo junto: `docker compose up --build` → http://localhost

---

## ⚠️ Peligros conocidos (no ignorar)

1. ~~Base de datos de Railway por defecto~~ y ~~secretos en `docker-compose.yml`~~: **resuelto** en la Fase 0 (MySQL local por defecto, secretos en `.env`, JWT nuevo). El secreto JWT antiguo sigue en el historial de git: no reutilizarlo nunca.
2. **El esquema de `init-db/dump.sql` no coincide con las entidades.** Al arrancar, Hibernate (`ddl-auto=update`) intenta cambiar `tipo_servicio.id` y `servicio_local.tipo_servicio_id` a `bigint`, falla por la clave foránea `fk_tipo_servicio` y deja un WARN. Pendiente: alinear el dump con las entidades (o pasar a migraciones con Flyway) y quitar `ddl-auto=update`.
3. ~~Sin token o con token caducado el backend respondía 403 en vez de 401~~: **resuelto** en la Fase 2. Sin sesión válida (sin token; token caducado, falsificado, mal formado o de un usuario borrado; login con credenciales incorrectas) → 401; con sesión pero sin permiso → 403. Los 401 llevan `WWW-Authenticate: Bearer`. La redirección interna a `/error` ya no convierte los errores en 403: llegan con su estado real. Fijado en `AutenticacionTest`. Consecuencia en el frontend: con un token caducado, el interceptor de `router/api.js` cierra la sesión y redirige a `/login`, también desde páginas públicas (se revisará en la Fase 5).
4. ~~CORS ligado a Railway y frontend local sin acceso al backend~~: **resuelto** (orígenes en `CORS_ALLOWED_ORIGINS` y proxy `/api` en Vite). ~~`authService.js` con URL a pelo~~: era código muerto, eliminado.
5. El token JWT se guarda en `localStorage`.
6. ~~Errores de negocio como 500 sin mensaje~~: **resuelto** en la Fase 2. Las reglas de negocio lanzan `ReglaDeNegocioException` (→ **409**) y los datos no válidos que no cubre `jakarta.validation` lanzan `PeticionNoValidaException` (→ **400**). `exception/ManejadorDeErrores` (`@RestControllerAdvice`) las traduce con el cuerpo `{"message": "..."}`, que el frontend ya muestra (`error.response.data.message`). Fijado en `ErroresApiTest`. Los errores de `jakarta.validation` (`MethodArgumentNotValidException`) también llegan como 400 con los mensajes de los DTOs, ordenados por campo y separados por un espacio (si un campo incumple varias reglas, por orden de mensaje). Registro, login y edición del perfil también se validan (todos los campos obligatorios y email válido, como el formulario de registro), y la administración no puede dejar a una clienta sin teléfono; fijado en `ValidacionAutenticacionTest`. Desde la Fase 3 también llegan con `message`: los 404 (`ResourceNotFoundException`), un parámetro que falta ("Falta el parámetro X.") o mal escrito ("El parámetro X no tiene un formato válido."), un cuerpo ilegible y un mes que no existe en los días no disponibles (400); fijado en `ErroresApiTest`.
6b. ~~Desfase horario en el panel de administración~~: **resuelto**. `CitaForm.jsx` enviaba la hora con `toISOString()` (las 10:00 llegaban como `09:00:00.000Z`); ahora envía la hora local sin zona (`yyyy-MM-ddTHH:mm:ss`), igual que la reserva de clientas. Fijado en `CitaForm.test.jsx`.
7. **`AuthContext` registra un interceptor de axios nuevo en cada render.** `logout` se crea de nuevo en cada render y está en las dependencias del `useEffect` que llama a `setupInterceptors`, así que los interceptores se acumulan y nunca se eliminan. Hay que caracterizarlo antes de corregirlo.
7b. ~~Reservas simultáneas~~: **resuelto** en la Fase 3. Reservar o mover una cita bloquea primero la fila de la trabajadora (`RepositorioTrabajador.findByIdBloqueando`, `SELECT ... FOR UPDATE`): las reservas de una misma trabajadora van de una en una y las de trabajadoras distintas siguen en paralelo. El bloqueo tiene que ser la primera lectura de la transacción (en MySQL la primera lectura normal fija la foto de los datos). Fijado en `ReservasSimultaneasTest`. Editar, borrar o rehacer los turnos y crear una ausencia toman el mismo bloqueo (en orden de id; editar y borrar turnos van en `READ COMMITTED` porque leen el turno antes de saber qué trabajadora bloquear), así que una reserva que se cruza con ellos no se cuela; fijado en `ReservasCruzadasTest`. **Queda abierto (Fase 4):** un bloqueo global no bloquea a ninguna trabajadora, así que una reserva cruzada con él todavía puede colarse.
8. **Lint:** `npm run lint` tiene 24 problemas que ya existían; se irán corrigiendo durante el refactor. El código nuevo o modificado debe pasar el lint.
10. **Pendientes encontrados al cerrar la Fase 2** (ya existían; caracterizar y corregir con `tdd`):
    - ~~Editar una clienta dejaba el rol a `null`~~: **resuelto** en la Fase 3. `/api/admin/clientes/{id}` solo trabaja con clientas (una administradora → 404), nunca cambia el rol, valida como el registro y no borra una clienta con citas (409). Fijado en `ClientasAdminApiTest`.
    - ~~Email duplicado → 500~~: **resuelto** en la Fase 3. El registro y la edición de clientas responden 409 "El email ya está en uso.", también en una carrera (traducen la `DataIntegrityViolationException` del `unique`; `ManejadorDeErrores` no la traduce de forma genérica a propósito, para no dar un mensaje falso con otras restricciones). Fijado en `EmailUnicoApiTest`.
    - ~~Los 404 sin `message`~~: **resuelto** en la Fase 3 (ver 6).
    - `@Data` de Lombok en entidades que se referencian entre sí (`Trabajador` ↔ `Turno`/`PeriodoNoDisponible`): `toString()`/`hashCode()` recursivos → `StackOverflowError` si se llaman con las colecciones cargadas.
9. Detalles menores: `RopositorioPeriodoNoDisp` (errata); `tailwind.config.cjs` usa `export default` (sintaxis ESM) con extensión `.cjs`, y `components.json` apunta a `tailwind.config.js`; `README.md` está en UTF-16.

No te conectes a la base de datos antigua de Railway ni a ningún servicio remoto. Todo se ejecuta contra el MySQL de docker en local.

Las reglas de negocio de las citas (`CitaService`) están en `backend-springboot/CLAUDE.md`.

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
- **Mutation testing al cierre de cada fase**, no tras cada paso: PIT en el backend y StrykerJS en el frontend (instalados al cerrar la Fase 1; comandos en "Comandos"). Se ejecutan sobre el código cambiado en la fase. Los mutantes equivalentes (que ningún test puede distinguir) se anotan y no se persiguen.

### Rediseño visual

Las reglas del rediseño están en `frontend-react/CLAUDE.md`.

---

## Testing

Las reglas de testing están en `backend-springboot/CLAUDE.md` (JUnit 5 + Testcontainers) y en `frontend-react/CLAUDE.md` (Vitest + jsdom + Testing Library).

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
