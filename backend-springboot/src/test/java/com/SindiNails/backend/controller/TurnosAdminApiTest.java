package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Turnos de las trabajadoras desde la administración (/api/admin/turnos y /api/admin/trabajadores/{id}/turnos).
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea su propia
 * trabajadora. Fechas fijas en 2030 (el lunes 2030-01-07 empieza la semana de los tests en lote).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class TurnosAdminApiTest {

    private static final LocalDate LUNES = LocalDate.of(2030, 1, 7);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioCita citas;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;
    private Trabajador sindi;

    @BeforeEach
    void unaAdministradoraYUnaTrabajadora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        sindi = datos.trabajadora("Sindi");
    }

    // ---------------------------------------------------------------- listado

    @Test
    void el_listado_de_turnos_de_una_trabajadora_sale_ordenado_por_fecha_y_hora() throws Exception {
        Trabajador ana = datos.trabajadora("Ana");
        datos.turno(sindi, LUNES.plusDays(1), "16:00", "20:00");
        datos.turno(sindi, LUNES, "16:00", "20:00");
        datos.turno(ana, LUNES, "09:00", "13:00");
        datos.turno(sindi, LUNES, "10:00", "14:00");

        assertThat(turnosDe(sindi)).containsExactly(
                "2030-01-07 10:00-14:00", "2030-01-07 16:00-20:00", "2030-01-08 16:00-20:00");
    }

    // ---------------------------------------------------------------- un turno acaba después de empezar

    @Test
    void crear_un_turno_que_acaba_antes_de_empezar_o_a_la_vez_responde_400_y_no_lo_guarda() throws Exception {
        for (String fin : List.of("09:00", "10:00")) {
            var r = crear(sindi, LUNES, "10:00", fin);

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).isEqualTo("La hora de fin debe ser posterior a la de inicio.");
        }
        assertThat(turnosDe(sindi)).isEmpty();
    }

    @Test
    void crear_un_turno_de_un_minuto_se_permite() throws Exception {
        assertThat(crear(sindi, LUNES, "10:00", "10:01").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void editar_un_turno_para_que_acabe_antes_de_empezar_responde_400_y_no_lo_cambia() throws Exception {
        Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));

        var r = editar(id, sindi, LUNES, "14:00", "10:00");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("La hora de fin debe ser posterior a la de inicio.");
        assertThat(turnosDe(sindi)).containsExactly("2030-01-07 10:00-14:00");
    }

    @Test
    void un_lote_con_un_dia_que_acaba_antes_de_empezar_responde_400_y_no_toca_la_semana() throws Exception {
        datos.turno(sindi, LUNES, "16:00", "20:00");

        var r = lote(sindi, """
                "0":{"activo":true,"horaInicio":"10:00","horaFin":"14:00"},
                "1":{"activo":true,"horaInicio":"14:00","horaFin":"10:00"}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("La hora de fin debe ser posterior a la de inicio.");
        assertThat(turnosDe(sindi)).containsExactly("2030-01-07 16:00-20:00");
    }

    @Test
    void un_lote_con_el_descanso_fuera_del_horario_o_al_reves_responde_400_y_no_toca_la_semana() throws Exception {
        datos.turno(sindi, LUNES, "16:00", "20:00");
        for (String descanso : List.of(
                "\"horaInicioDescanso\":\"15:00\",\"horaFinDescanso\":\"16:00\"",
                "\"horaInicioDescanso\":\"09:00\",\"horaFinDescanso\":\"11:00\"",
                "\"horaInicioDescanso\":\"12:00\",\"horaFinDescanso\":\"11:00\"",
                "\"horaInicioDescanso\":\"10:00\",\"horaFinDescanso\":\"11:00\"",
                "\"horaInicioDescanso\":\"13:00\",\"horaFinDescanso\":\"14:00\"")) {
            var r = lote(sindi, """
                    "0":{"activo":true,"horaInicio":"10:00","horaFin":"14:00",%s}
                    """.formatted(descanso));

            assertThat(r.getStatusCode()).as(descanso).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).isEqualTo("El descanso debe empezar y acabar dentro del horario.");
        }
        assertThat(turnosDe(sindi)).containsExactly("2030-01-07 16:00-20:00");
    }

    // ---------------------------------------------------------------- turnos solapados

    @Test
    void crear_un_turno_que_se_solapa_con_otro_de_la_trabajadora_responde_409_y_no_lo_guarda() throws Exception {
        datos.turno(sindi, LUNES, "10:00", "14:00");

        for (String[] horas : List.of(new String[]{"12:00", "16:00"}, new String[]{"08:00", "11:00"},
                new String[]{"11:00", "12:00"}, new String[]{"09:00", "15:00"}, new String[]{"10:00", "14:00"})) {
            var r = crear(sindi, LUNES, horas[0], horas[1]);

            assertThat(r.getStatusCode()).as(horas[0] + "-" + horas[1]).isEqualTo(HttpStatus.CONFLICT);
            assertThat(mensaje(r)).isEqualTo("El turno se solapa con otro turno de la trabajadora ese día.");
        }
        assertThat(turnosDe(sindi)).containsExactly("2030-01-07 10:00-14:00");
    }

    @Test
    void los_turnos_contiguos_otro_dia_o_de_otra_trabajadora_no_se_solapan() throws Exception {
        datos.turno(sindi, LUNES, "10:00", "14:00");
        Trabajador ana = datos.trabajadora("Ana");

        assertThat(crear(sindi, LUNES, "14:00", "18:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crear(sindi, LUNES, "08:00", "10:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crear(sindi, LUNES.plusDays(1), "10:00", "14:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(crear(ana, LUNES, "10:00", "14:00").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void editar_un_turno_sin_moverlo_o_alargandolo_sobre_si_mismo_se_permite() throws Exception {
        Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));

        assertThat(editar(id, sindi, LUNES, "10:00", "14:00").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(editar(id, sindi, LUNES, "09:00", "15:00").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void editar_un_turno_para_que_se_solape_con_otro_responde_409_y_no_lo_cambia() throws Exception {
        datos.turno(sindi, LUNES, "16:00", "20:00");
        Trabajador ana = datos.trabajadora("Ana");
        datos.turno(ana, LUNES, "10:00", "14:00");
        Long id = idDe(crear(sindi, LUNES, "10:00", "14:00"));

        var mismaTrabajadora = editar(id, sindi, LUNES, "12:00", "17:00");
        var otraTrabajadora = editar(id, ana, LUNES, "12:00", "15:00");

        assertThat(mismaTrabajadora.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(otraTrabajadora.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(otraTrabajadora)).isEqualTo("El turno se solapa con otro turno de la trabajadora ese día.");
        assertThat(turnosDe(sindi)).containsExactly("2030-01-07 10:00-14:00", "2030-01-07 16:00-20:00");
    }

    // ---------------------------------------------------------------- crear la semana en lote

    @Test
    void rehacer_la_semana_sustituye_solo_los_turnos_de_esa_semana() throws Exception {
        datos.turno(sindi, LUNES.minusDays(1), "16:00", "20:00");
        datos.turno(sindi, LUNES.plusDays(2), "16:00", "20:00");
        datos.turno(sindi, LUNES.plusDays(7), "16:00", "20:00");

        var r = lote(sindi, """
                "0":{"activo":true,"horaInicio":"10:00","horaFin":"14:00"},
                "1":{"activo":true,"horaInicio":"09:00","horaFin":"18:00","horaInicioDescanso":"13:00","horaFinDescanso":"14:00"},
                "2":{"activo":false}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(turnosDe(sindi)).containsExactly(
                "2030-01-06 16:00-20:00",
                "2030-01-07 10:00-14:00",
                "2030-01-08 09:00-13:00", "2030-01-08 14:00-18:00",
                "2030-01-14 16:00-20:00");
    }

    // ---------------------------------------------------------------- utilidades

    private ResponseEntity<String> crear(Trabajador t, LocalDate fecha, String inicio, String fin) {
        return admin(HttpMethod.POST, "/api/admin/turnos", turnoJson(t, fecha, inicio, fin));
    }

    private ResponseEntity<String> editar(Long id, Trabajador t, LocalDate fecha, String inicio, String fin) {
        return admin(HttpMethod.PUT, "/api/admin/turnos/" + id, turnoJson(t, fecha, inicio, fin));
    }

    private static String turnoJson(Trabajador t, LocalDate fecha, String inicio, String fin) {
        return """
                {"trabajadorId":%d,"fecha":"%s","horaInicio":"%s","horaFin":"%s"}
                """.formatted(t.getId(), fecha, inicio, fin);
    }

    private Long idDe(ResponseEntity<String> r) throws Exception {
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return json.readTree(r.getBody()).path("id").asLong();
    }

    private ResponseEntity<String> lote(Trabajador t, String horarios) {
        return admin(HttpMethod.POST, "/api/admin/turnos/batch", """
                {"trabajadorId":%d,"fechaInicioSemana":"%s","horarios":{%s}}
                """.formatted(t.getId(), LUNES, horarios));
    }

    /** Los turnos del listado de la trabajadora, como "fecha inicio-fin", en el orden en que llegan. */
    private List<String> turnosDe(Trabajador t) throws Exception {
        var r = admin(HttpMethod.GET, "/api/admin/trabajadores/" + t.getId() + "/turnos", null);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode lista = json.readTree(r.getBody());
        return StreamSupport.stream(lista.spliterator(), false)
                .map(n -> n.path("fecha").asText() + " " + n.path("horaInicio").asText().substring(0, 5)
                        + "-" + n.path("horaFin").asText().substring(0, 5))
                .toList();
    }

    private String mensaje(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("message").asText(null);
    }

    private ResponseEntity<String> admin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }
}
