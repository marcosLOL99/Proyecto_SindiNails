package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioTurno;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el comportamiento ACTUAL de los turnos de las trabajadoras
 * (TurnoService y /api/admin/turnos, /api/admin/trabajadores/{id}/turnos), NO lo que debería hacer.
 *
 * Fijan lo que pasa hoy para poder corregir los bugs con TDD y refactorizar con seguridad. Los casos
 * marcados como SUSPICIOUS parecen bugs: se documentan tal cual y se deciden con el usuario.
 *
 * Son temporales: se sustituirán por tests de comportamiento al corregir y refactorizar.
 * Ver la skill characterisation-tests.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea su
 * propia trabajadora, así que sus turnos no se mezclan con los de otros tests. Fechas fijas en 2030
 * (el lunes 2030-01-07 empieza la semana de los tests en lote).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class TurnosAdminCharacterisationTest {

    private static final LocalDate LUNES = LocalDate.of(2030, 1, 7);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioTurno turnos;
    @Autowired RepositorioCita citas;
    @Autowired JwtService jwtService;

    private Usuario admin;
    private Trabajador sindi;

    @BeforeEach
    void unaAdministradoraYUnaTrabajadora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        sindi = datos.trabajadora("Sindi");
    }

    // ---------------------------------------------------------------- listar

    @Nested
    class Listar {

        @Test
        void characterises_trabajadora_sin_turnos() {
            assertThat(listar(sindi)).isEqualTo("200 []");
        }

        @Test
        void characterises_trabajadora_que_no_existe() {
            assertThat(resumen(admin(HttpMethod.GET, "/api/admin/trabajadores/99999999/turnos", null)))
                    .isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/trabajadores/N/turnos\"}");
        }
    }

    // ---------------------------------------------------------------- crear

    @Nested
    class Crear {

        @Test
        void characterises_crear_un_turno() {
            assertThat(resumen(crear(sindi, LUNES, "10:00", "14:00"))).isEqualTo("201 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}");
        }

        @Test
        void characterises_crear_sin_datos() {
            assertThat(resumen(admin(HttpMethod.POST, "/api/admin/turnos", "{}"))).isEqualTo("400 {\"message\":\"La fecha es obligatoria. La hora de fin es obligatoria. La hora de inicio es obligatoria. El ID del trabajador es obligatorio.\"}");
        }

        @Test
        void characterises_crear_un_turno_que_se_solapa_con_otro() {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            // SUSPICIOUS (T2): se aceptan dos turnos solapados de la misma trabajadora el mismo día.
            assertThat(resumen(crear(sindi, LUNES, "12:00", "16:00"))).isEqualTo("201 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"12:00:00\",\"horaFin\":\"16:00:00\"}");
            assertThat(listar(sindi)).isEqualTo("200 [{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"},{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"12:00:00\",\"horaFin\":\"16:00:00\"}]");
        }

        @Test
        void characterises_crear_un_turno_en_el_pasado() {
            assertThat(resumen(crear(sindi, LocalDate.of(2020, 1, 6), "10:00", "14:00"))).isEqualTo("201 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2020-01-06\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}");
        }

        @Test
        void characterises_crear_para_una_trabajadora_inactiva() {
            Trabajador inactiva = datos.trabajadoraInactiva("Lola");
            assertThat(resumen(crear(inactiva, LUNES, "10:00", "14:00"))).isEqualTo("201 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Lola\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}");
        }

        @Test
        void characterises_crear_para_una_trabajadora_que_no_existe() {
            var r = admin(HttpMethod.POST, "/api/admin/turnos", """
                    {"trabajadorId":99999999,"fecha":"2030-01-07","horaInicio":"10:00","horaFin":"14:00"}
                    """);
            assertThat(resumen(r)).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/turnos\"}");
        }
    }

    // ---------------------------------------------------------------- editar

    @Nested
    class Editar {

        @Test
        void characterises_editar_un_turno() {
            Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));
            assertThat(resumen(editar(id, sindi, LUNES.plusDays(1), "09:00", "13:00"))).isEqualTo("200 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-08\",\"horaInicio\":\"09:00:00\",\"horaFin\":\"13:00:00\"}");
            assertThat(listar(sindi)).isEqualTo("200 [{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-08\",\"horaInicio\":\"09:00:00\",\"horaFin\":\"13:00:00\"}]");
        }

        @Test
        void characterises_editar_pasandolo_a_otra_trabajadora() {
            Trabajador ana = datos.trabajadora("Ana");
            Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));
            assertThat(resumen(editar(id, ana, LUNES, "10:00", "14:00"))).isEqualTo("200 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Ana\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}");
            assertThat(listar(sindi)).isEqualTo("200 []");
            assertThat(listar(ana)).isEqualTo("200 [{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Ana\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}]");
        }

        @Test
        void characterises_editar_un_turno_que_no_existe() {
            assertThat(resumen(editar(99999999L, sindi, LUNES, "10:00", "14:00"))).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/turnos/N\"}");
        }

        @Test
        void characterises_editar_un_turno_dejando_fuera_una_cita() {
            Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));
            Cita cita = citaConfirmada(LUNES.atTime(12, 0));
            // SUSPICIOUS (T3): la cita confirmada se queda fuera de turno, sin aviso.
            assertThat(resumen(editar(id, sindi, LUNES, "10:00", "11:00"))).isEqualTo("200 {\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"11:00:00\"}");
            assertThat(estadoDe(cita)).isEqualTo("CONFIRMADA fuera de turno");
        }
    }

    // ---------------------------------------------------------------- borrar

    @Nested
    class Borrar {

        @Test
        void characterises_borrar_un_turno() {
            Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));
            assertThat(resumen(admin(HttpMethod.DELETE, "/api/admin/turnos/" + id, null))).isEqualTo("204 null");
            assertThat(listar(sindi)).isEqualTo("200 []");
        }

        @Test
        void characterises_borrar_un_turno_con_una_cita_dentro() {
            Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));
            Cita cita = citaConfirmada(LUNES.atTime(12, 0));
            // SUSPICIOUS (T3): la cita confirmada se queda fuera de turno, sin aviso.
            assertThat(resumen(admin(HttpMethod.DELETE, "/api/admin/turnos/" + id, null))).isEqualTo("204 null");
            assertThat(estadoDe(cita)).isEqualTo("CONFIRMADA fuera de turno");
        }

        @Test
        void characterises_borrar_un_turno_que_no_existe() {
            assertThat(resumen(admin(HttpMethod.DELETE, "/api/admin/turnos/99999999", null))).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/turnos/N\"}");
        }
    }

    // ---------------------------------------------------------------- crear en lote (semana)

    @Nested
    class EnLote {

        @Test
        void characterises_semana_con_un_dia_normal_uno_con_descanso_y_uno_inactivo() {
            var r = lote(sindi, """
                    "0":{"activo":true,"horaInicio":"10:00","horaFin":"14:00"},
                    "1":{"activo":true,"horaInicio":"09:00","horaFin":"18:00","horaInicioDescanso":"13:00","horaFinDescanso":"14:00"},
                    "2":{"activo":false,"horaInicio":"09:00","horaFin":"18:00"}
                    """);
            assertThat(resumen(r)).isEqualTo("201 [{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-07\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"},{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-08\",\"horaInicio\":\"09:00:00\",\"horaFin\":\"13:00:00\"},{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-08\",\"horaInicio\":\"14:00:00\",\"horaFin\":\"18:00:00\"}]");
        }

        @Test
        void characterises_lote_con_un_dia_fuera_de_la_semana() {
            var r = lote(sindi, """
                    "7":{"activo":true,"horaInicio":"10:00","horaFin":"14:00"},
                    "-1":{"activo":true,"horaInicio":"10:00","horaFin":"14:00"}
                    """);
            // SUSPICIOUS (T4): los días 7 y -1 crean turnos fuera de la semana (y esos no se sustituyen al repetir el lote).
            assertThat(resumen(r)).isEqualTo("201 [{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-14\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"},{\"id\":N,\"trabajadorId\":N,\"trabajadorNombre\":\"Sindi\",\"fecha\":\"2030-01-06\",\"horaInicio\":\"10:00:00\",\"horaFin\":\"14:00:00\"}]");
        }

        @Test
        void characterises_lote_con_un_dia_activo_sin_horas() {
            var r = lote(sindi, """
                    "0":{"activo":true}
                    """);
            // SUSPICIOUS (T6): un día marcado como activo sin horas se ignora en silencio.
            assertThat(resumen(r)).isEqualTo("201 []");
        }

        @Test
        void characterises_lote_sin_horarios() {
            var r = admin(HttpMethod.POST, "/api/admin/turnos/batch", """
                    {"trabajadorId":%d,"fechaInicioSemana":"2030-01-07"}
                    """.formatted(sindi.getId()));
            // SUSPICIOUS (T5): sin horarios da 500 (NullPointerException) en vez de 400.
            assertThat(resumen(r)).isEqualTo("500 {\"timestamp\":T,\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/admin/turnos/batch\"}");
            assertThat(listar(sindi)).isEqualTo("200 []");
        }

        @Test
        void characterises_lote_sin_datos() {
            // SUSPICIOUS (T6): los mensajes son los genéricos de Spring ("no debe ser nulo"), sin decir qué campo falta.
            assertThat(resumen(admin(HttpMethod.POST, "/api/admin/turnos/batch", "{}"))).isEqualTo("400 {\"message\":\"no debe ser nulo no debe ser nulo\"}");
        }

        @Test
        void characterises_lote_que_quita_el_turno_de_una_cita() {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            Cita cita = citaConfirmada(LUNES.atTime(12, 0));
            var r = lote(sindi, """
                    "0":{"activo":false}
                    """);
            // SUSPICIOUS (T3): la cita confirmada se queda fuera de turno, sin aviso.
            assertThat(resumen(r)).isEqualTo("201 []");
            assertThat(estadoDe(cita)).isEqualTo("CONFIRMADA fuera de turno");
        }

        @Test
        void characterises_lote_para_una_trabajadora_que_no_existe() {
            var r = admin(HttpMethod.POST, "/api/admin/turnos/batch", """
                    {"trabajadorId":99999999,"fechaInicioSemana":"2030-01-07","horarios":{}}
                    """);
            assertThat(resumen(r)).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/turnos/batch\"}");
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Cita citaConfirmada(LocalDateTime inicio) {
        Usuario clienta = datos.clienta("Laura");
        return datos.cita(clienta, sindi, datos.servicio("Semipermanente", 60), inicio, EstadoCita.CONFIRMADA);
    }

    /** La cita sigue existiendo, con su estado y si cae dentro de algún turno de su trabajadora ese día. */
    private String estadoDe(Cita cita) {
        return citas.findById(cita.getId()).map(c -> {
            boolean dentro = turnos.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(
                            sindi.getId(), c.getFechaHoraInicio().toLocalDate()).stream()
                    .anyMatch(t -> !c.getFechaHoraInicio().toLocalTime().isBefore(t.getHoraInicio())
                            && !c.getFechaHoraFin().toLocalTime().isAfter(t.getHoraFin()));
            return c.getEstado() + (dentro ? " dentro de un turno" : " fuera de turno");
        }).orElse("borrada");
    }

    private ResponseEntity<String> crear(Trabajador t, LocalDate fecha, String inicio, String fin) {
        return admin(HttpMethod.POST, "/api/admin/turnos", turnoJson(t, fecha, inicio, fin));
    }

    private ResponseEntity<String> editar(Long id, Trabajador t, LocalDate fecha, String inicio, String fin) {
        return admin(HttpMethod.PUT, "/api/admin/turnos/" + id, turnoJson(t, fecha, inicio, fin));
    }

    private ResponseEntity<String> lote(Trabajador t, String horarios) {
        return admin(HttpMethod.POST, "/api/admin/turnos/batch", """
                {"trabajadorId":%d,"fechaInicioSemana":"%s","horarios":{%s}}
                """.formatted(t.getId(), LUNES, horarios));
    }

    private static String turnoJson(Trabajador t, LocalDate fecha, String inicio, String fin) {
        return """
                {"trabajadorId":%d,"fecha":"%s","horaInicio":"%s","horaFin":"%s"}
                """.formatted(t.getId(), fecha, inicio, fin);
    }

    private String listar(Trabajador t) {
        return resumen(admin(HttpMethod.GET, "/api/admin/trabajadores/" + t.getId() + "/turnos", null));
    }

    private static Long idDe(ResponseEntity<String> r) {
        return Long.valueOf(r.getBody().replaceAll("(?s).*?\"id\":(\\d+).*", "$1"));
    }

    private ResponseEntity<String> admin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    /** Estado y cuerpo en una línea, sin ids ni marcas de tiempo. */
    private static String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + String.valueOf(r.getBody())
                .replaceAll("\"id\":\\d+", "\"id\":N")
                .replaceAll("\"trabajadorId\":\\d+", "\"trabajadorId\":N")
                .replaceAll("/\\d+", "/N")
                .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":T");
    }
}
