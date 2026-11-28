# CLAUDE.md — backend (SindiNails)

Complementa el `CLAUDE.md` de la raíz (peligros, comandos, skills, convenciones y Git siguen allí).

## Reglas de negocio de las citas (`CitaService`)

Fijadas por tests permanentes en `CitaServiceTest` (reloj congelado con `RelojFijo`). "Ahora" es la hora del salón: el `Clock` de `ClockConfig` usa `app.zona-horaria` (`Europe/Madrid`), no la zona del sistema (en docker la JVM está en UTC). Citas y turnos se guardan como hora local del salón. Los bugs S1–S7 encontrados al caracterizar en la Fase 1 se corrigieron así:

- **Reservar o mover una cita** (también la administración): no en el pasado; trabajadora activa; la cita entera dentro de uno de sus turnos de ese día; sin pisar ausencias de la trabajadora ni bloqueos globales; sin solaparse con otra cita no cancelada. No hace falta empezar en un cuarto de hora. Una cita cancelada no se puede mover. **Excepción:** si la administración edita una cita sin cambiar su hora de inicio (corregir servicio o trabajadora), no se aplican las reglas de horario (pasado, activa, turno, ausencias), así se pueden corregir citas pasadas; el solape con otras citas sí se comprueba siempre.
- **Huecos libres**: cada 15 minutos dentro de los turnos, ordenados por hora; nunca huecos que ya han pasado; ninguno para una trabajadora inactiva; con turnos que acaban cerca de medianoche, solo huecos que terminan ese mismo día (antes el cálculo daba la vuelta a las 00:00 y no terminaba nunca).
- **Cancelar** (clienta): solo la dueña (a otra clienta se le responde 404, como si la cita no existiera, para no revelar qué citas existen), con 12 horas o más de antelación (justo 12 h vale); no una cita ya cancelada ni una que ya ha pasado (cada caso con su mensaje).
- **Resumen semanal del panel**: últimos 7 días (desde hace 6 días a las 00:00), sin contar las canceladas.

## Reglas de turnos, ausencias y bloqueos (`TurnoService`, `PeriodoNoDisponibleService`)

Fijadas en la Fase 3 por `TurnosAdminApiTest` y `BloqueosAdminApiTest`:

- **Turnos**: acaban después de empezar (400); dos turnos de la misma trabajadora el mismo día no se solapan (409; contiguos sí). Los listados salen por fecha y hora.
- **Semana en lote** (`/turnos/batch`): días del 0 (lunes) al 6 (domingo); horarios obligatorios; un día activo necesita sus horas; el descanso cae estrictamente dentro del horario (400). Sustituye solo los turnos de esa semana, y nada se borra si la petición no es válida.
- **Citas protegidas**: editar o borrar un turno, o rehacer la semana, no puede dejar una cita sin cancelar y todavía por empezar fuera de turno; crear una ausencia no puede pisar las de su trabajadora, ni un bloqueo global las de ninguna (409 con mensaje). Las citas pasadas no cuentan.
- **Ausencias y bloqueos**: acaban después de empezar (400); las ausencias de una trabajadora que no existe → 404.
- Un cuerpo ilegible (JSON mal formado, fecha mal escrita, tipo desconocido) → 400 "La petición no tiene un formato válido." (`ManejadorDeErrores`).

En los tests HTTP la base de datos es compartida entre clases: un bloqueo global choca con las citas que otros tests dejan, así que los tests que crean bloqueos globales válidos usan días propios (2031).

Lo que todavía no tiene test permanente sigue en `CitaServiceCharacterisationTest` (huecos con citas y bloqueos, días no disponibles, borrar, listados): se sustituirá al refactorizar `CitaService`.

Notas de diseño para el refactor: la regla "el hueco cabe en un turno y no pisa ausencias, bloqueos ni citas" está escrita dos veces, en `validarHorarioReservable` (al reservar) y en `getHorariosDisponibles` + `isOverlapping` (al ofrecer huecos); conviene una sola comprobación para que lo que se ofrece y lo que se acepta no se separen; `getDiasNoDisponibles` llama a `getHorariosDisponibles` 42 veces, con varias consultas en cada llamada; `createCita` y `updateCita` repiten la carga de servicio y trabajadora y el cálculo del fin.

## Testing — JUnit 5 + Spring Boot Test + Testcontainers (MySQL 8)

- Todo test de Spring lleva `@ActiveProfiles("test")` + `@Import(TestcontainersConfiguration.class)`. Así usa un MySQL 8 desechable (`@ServiceConnection`) y los valores fijos de `src/test/resources/application-test.properties` (incluido un secreto JWT solo para tests). No depende del `.env`.
- Los tests de integración nunca usan el MySQL de desarrollo ni ninguna base de datos remota. La base de datos del contenedor empieza vacía (sin `dump.sql`): cada test crea sus propios datos.
- `testcontainers.version` está fijada a 1.21.4 en el `pom.xml`, porque la 1.21.3 que trae Spring Boot no funciona con Docker Engine 29. No la quites al actualizar Spring Boot salvo que este ya traiga una versión igual o superior.
- Prioridad de caracterización: `CitaService` (disponibilidad de horarios, días no disponibles, solapes con turnos y ausencias, cancelación), autenticación/JWT y permisos por rol (`/api/admin/**` solo para ADMIN).
- Para endpoints: `@SpringBootTest` + MockMvc, comprobando estado HTTP, cuerpo JSON y seguridad (401/403).
