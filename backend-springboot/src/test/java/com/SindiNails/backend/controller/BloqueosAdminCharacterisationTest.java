package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el comportamiento ACTUAL de las ausencias de las trabajadoras y los
 * bloqueos globales del salón (PeriodoNoDisponibleService y /api/admin/bloqueos), NO lo que debería hacer.
 *
 * Fijan lo que pasa hoy para poder corregir los bugs con TDD y refactorizar con seguridad. Los casos
 * marcados como SUSPICIOUS parecen bugs: se documentan tal cual y se deciden con el usuario.
 *
 * Son temporales: se sustituirán por tests de comportamiento al corregir y refactorizar.
 * Ver la skill characterisation-tests.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea su
 * trabajadora, y los bloqueos globales llevan un motivo único para encontrarlos en el listado compartido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class BloqueosAdminCharacterisationTest {

    private static final LocalDateTime DIA = LocalDateTime.of(2030, 1, 7, 0, 0);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioCita citas;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;
    private Trabajador sindi;
    private String motivo;

    @BeforeEach
    void unaAdministradoraYUnaTrabajadora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        sindi = datos.trabajadora("Sindi");
        motivo = "Motivo " + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------------------------------------------------------------- crear

    @Nested
    class Crear {

        @Test
        void characterises_crear_una_ausencia_de_trabajadora() {
            assertThat(resumen(ausencia(sindi, DIA.withHour(10), DIA.withHour(12)))).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T10:00:00\",\"fechaHoraFin\":\"2030-01-07T12:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"TRABAJADOR\",\"trabajadorId\":N}");
        }

        @Test
        void characterises_crear_un_bloqueo_global() {
            assertThat(resumen(global(DIA.withHour(10), DIA.withHour(12)))).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T10:00:00\",\"fechaHoraFin\":\"2030-01-07T12:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"GLOBAL\",\"trabajadorId\":null}");
        }

        @Test
        void characterises_crear_un_bloqueo_global_con_trabajadora() {
            var r = admin(HttpMethod.POST, "/api/admin/bloqueos", """
                    {"fechaHoraInicio":"2030-01-07T10:00:00","fechaHoraFin":"2030-01-07T12:00:00",
                     "motivo":"%s","tipo":"GLOBAL","trabajadorId":%d}
                    """.formatted(motivo, sindi.getId()));
            assertThat(resumen(r)).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T10:00:00\",\"fechaHoraFin\":\"2030-01-07T12:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"GLOBAL\",\"trabajadorId\":null}");
        }

        @Test
        void characterises_crear_en_el_pasado() {
            assertThat(resumen(ausencia(sindi, LocalDateTime.of(2020, 1, 6, 10, 0), LocalDateTime.of(2020, 1, 6, 12, 0))))
                    .isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2020-01-06T10:00:00\",\"fechaHoraFin\":\"2020-01-06T12:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"TRABAJADOR\",\"trabajadorId\":N}");
        }

        @Test
        void characterises_crear_para_una_trabajadora_que_no_existe() {
            var r = admin(HttpMethod.POST, "/api/admin/bloqueos", """
                    {"fechaHoraInicio":"2030-01-07T10:00:00","fechaHoraFin":"2030-01-07T12:00:00",
                     "motivo":"%s","tipo":"TRABAJADOR","trabajadorId":99999999}
                    """.formatted(motivo));
            assertThat(resumen(r)).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/bloqueos\"}");
        }

        @Test
        void characterises_ausencia_que_pisa_una_cita_confirmada() {
            datos.turno(sindi, DIA.toLocalDate(), "10:00", "14:00");
            Cita cita = citaConfirmada(DIA.withHour(11));
            // SUSPICIOUS (B2): la cita confirmada sigue en pie dentro de la ausencia, sin aviso.
            assertThat(resumen(ausencia(sindi, DIA.withHour(10), DIA.withHour(14)))).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T10:00:00\",\"fechaHoraFin\":\"2030-01-07T14:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"TRABAJADOR\",\"trabajadorId\":N}");
            assertThat(estadoDe(cita)).isEqualTo("CONFIRMADA");
        }

        @Test
        void characterises_bloqueo_global_que_pisa_una_cita_confirmada() {
            datos.turno(sindi, DIA.toLocalDate(), "10:00", "14:00");
            Cita cita = citaConfirmada(DIA.withHour(11));
            // SUSPICIOUS (B2): la cita confirmada sigue en pie con el salón cerrado, sin aviso.
            assertThat(resumen(global(DIA, DIA.plusDays(1)))).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T00:00:00\",\"fechaHoraFin\":\"2030-01-08T00:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"GLOBAL\",\"trabajadorId\":null}");
            assertThat(estadoDe(cita)).isEqualTo("CONFIRMADA");
        }

        @Test
        void characterises_dos_ausencias_solapadas() {
            ausencia(sindi, DIA.withHour(10), DIA.withHour(14));
            assertThat(resumen(ausencia(sindi, DIA.withHour(12), DIA.withHour(16)))).isEqualTo("201 {\"id\":N,\"fechaHoraInicio\":\"2030-01-07T12:00:00\",\"fechaHoraFin\":\"2030-01-07T16:00:00\",\"motivo\":\"MOTIVO\",\"tipo\":\"TRABAJADOR\",\"trabajadorId\":N}");
        }
    }

    // ---------------------------------------------------------------- listar

    @Nested
    class Listar {
    }

    // ---------------------------------------------------------------- borrar

    @Nested
    class Borrar {

        @Test
        void characterises_borrar_una_ausencia() throws Exception {
            Long id = idDe(ausencia(sindi, DIA.withHour(10), DIA.withHour(12)));
            assertThat(resumen(admin(HttpMethod.DELETE, "/api/admin/bloqueos/" + id, null))).isEqualTo("204 null");
            assertThat(resumen(admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + sindi.getId(), null)))
                    .isEqualTo("200 []");
        }

        @Test
        void characterises_borrar_un_bloqueo_que_no_existe() {
            assertThat(resumen(admin(HttpMethod.DELETE, "/api/admin/bloqueos/99999999", null))).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/bloqueos/N\"}");
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Cita citaConfirmada(LocalDateTime inicio) {
        return datos.cita(datos.clienta("Laura"), sindi, datos.servicio("Semipermanente", 60), inicio,
                EstadoCita.CONFIRMADA);
    }

    private String estadoDe(Cita cita) {
        return citas.findById(cita.getId()).map(c -> c.getEstado().toString()).orElse("borrada");
    }

    private ResponseEntity<String> ausencia(Trabajador t, LocalDateTime inicio, LocalDateTime fin) {
        return admin(HttpMethod.POST, "/api/admin/bloqueos", """
                {"fechaHoraInicio":"%s","fechaHoraFin":"%s","motivo":"%s","tipo":"TRABAJADOR","trabajadorId":%d}
                """.formatted(inicio, fin, motivo, t.getId()));
    }

    private ResponseEntity<String> global(LocalDateTime inicio, LocalDateTime fin) {
        return admin(HttpMethod.POST, "/api/admin/bloqueos", """
                {"fechaHoraInicio":"%s","fechaHoraFin":"%s","motivo":"%s","tipo":"GLOBAL"}
                """.formatted(inicio, fin, motivo));
    }

    private Long idDe(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("id").asLong();
    }

    private ResponseEntity<String> admin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + enmascarar(String.valueOf(r.getBody()));
    }

    /** Sin ids, marcas de tiempo ni el motivo aleatorio. */
    private String enmascarar(String texto) {
        return texto
                .replace(motivo, "MOTIVO")
                .replaceAll("\"id\":\\d+", "\"id\":N")
                .replaceAll("\"trabajadorId\":\\d+", "\"trabajadorId\":N")
                .replaceAll("/\\d+", "/N")
                .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":T");
    }
}
