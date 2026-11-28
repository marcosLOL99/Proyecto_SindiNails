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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ausencias de las trabajadoras y bloqueos globales del salón desde la administración (/api/admin/bloqueos).
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea su
 * trabajadora, y los bloqueos llevan un motivo único para encontrarlos en el listado compartido de globales.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class BloqueosAdminApiTest {

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

    // ---------------------------------------------------------------- listados

    @Test
    void las_ausencias_de_una_trabajadora_salen_ordenadas_por_fecha_y_hora() throws Exception {
        ausencia(sindi, DIA.plusDays(1).withHour(10), DIA.plusDays(1).withHour(12));
        ausencia(sindi, DIA.withHour(16), DIA.withHour(18));
        ausencia(sindi, DIA.withHour(10), DIA.withHour(12));

        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + sindi.getId(), null)))
                .containsExactly("2030-01-07T10:00:00", "2030-01-07T16:00:00", "2030-01-08T10:00:00");
    }

    @Test
    void los_bloqueos_globales_salen_ordenados_por_fecha_y_hora() throws Exception {
        global(DIA.plusDays(1).withHour(10), DIA.plusDays(1).withHour(12));
        global(DIA.withHour(16), DIA.withHour(18));
        global(DIA.withHour(10), DIA.withHour(12));

        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/globales", null)))
                .containsExactly("2030-01-07T10:00:00", "2030-01-07T16:00:00", "2030-01-08T10:00:00");
    }

    @Test
    void las_ausencias_de_una_trabajadora_que_no_existe_responden_404() {
        var r = admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/99999999", null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void una_trabajadora_sin_ausencias_tiene_el_listado_vacio() {
        var r = admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + sindi.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEqualTo("[]");
    }

    // ---------------------------------------------------------------- un periodo acaba después de empezar

    @Test
    void una_ausencia_o_un_bloqueo_que_acaba_antes_de_empezar_o_a_la_vez_responde_400_y_no_se_guarda() throws Exception {
        for (var r : List.of(
                ausencia(sindi, DIA.withHour(12), DIA.withHour(10)),
                ausencia(sindi, DIA.withHour(10), DIA.withHour(10)),
                global(DIA.withHour(12), DIA.withHour(10)),
                global(DIA.withHour(10), DIA.withHour(10)))) {
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).isEqualTo("La fecha y hora de fin deben ser posteriores a las de inicio.");
        }
        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + sindi.getId(), null))).isEmpty();
        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/globales", null))).isEmpty();
    }

    @Test
    void una_ausencia_de_varios_dias_se_permite() {
        var r = ausencia(sindi, DIA.withHour(10), DIA.plusDays(3).withHour(10));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---------------------------------------------------------------- utilidades

    /** Inicio de cada periodo del listado con el motivo de este test, en el orden en que llegan. */
    private List<String> inicios(ResponseEntity<String> r) throws Exception {
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode lista = json.readTree(r.getBody());
        return StreamSupport.stream(lista.spliterator(), false)
                .filter(n -> motivo.equals(n.path("motivo").asText()))
                .map(n -> n.path("fechaHoraInicio").asText())
                .toList();
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
