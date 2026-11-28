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

    /*
     * Un bloqueo global choca con las citas de cualquier trabajadora, y la base de datos de los tests es compartida
     * (otros tests dejan citas en 2030). Los tests que crean bloqueos globales válidos usan días propios de 2031.
     */
    private static final LocalDateTime DIA_GLOBALES_ORDENADOS = LocalDateTime.of(2031, 2, 3, 0, 0);
    private static final LocalDateTime DIA_GLOBAL_CON_CITA = LocalDateTime.of(2031, 3, 3, 0, 0);

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
        LocalDateTime dia = DIA_GLOBALES_ORDENADOS;
        global(dia.plusDays(1).withHour(10), dia.plusDays(1).withHour(12));
        global(dia.withHour(16), dia.withHour(18));
        global(dia.withHour(10), dia.withHour(12));

        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/globales", null)))
                .containsExactly("2031-02-03T10:00:00", "2031-02-03T16:00:00", "2031-02-04T10:00:00");
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

    // ---------------------------------------------------------------- mensajes

    @Test
    void crear_sin_datos_responde_400_diciendo_que_falta() throws Exception {
        var r = admin(HttpMethod.POST, "/api/admin/bloqueos", "{}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("La fecha y hora de fin son obligatorias. La fecha y hora de inicio son "
                + "obligatorias. El motivo no puede estar vacío. El tipo es obligatorio.");
    }

    @Test
    void un_tipo_desconocido_o_una_fecha_mal_escrita_responde_400_con_mensaje() throws Exception {
        for (String cuerpo : List.of(
                "{\"fechaHoraInicio\":\"2030-01-07T10:00:00\",\"fechaHoraFin\":\"2030-01-07T12:00:00\",\"motivo\":\"x\",\"tipo\":\"VACACIONES\"}",
                "{\"fechaHoraInicio\":\"mañana\",\"fechaHoraFin\":\"2030-01-07T12:00:00\",\"motivo\":\"x\",\"tipo\":\"GLOBAL\"}",
                "{esto no es json")) {
            var r = admin(HttpMethod.POST, "/api/admin/bloqueos", cuerpo);

            assertThat(r.getStatusCode()).as(cuerpo).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).isEqualTo("La petición no tiene un formato válido.");
        }
    }

    // ---------------------------------------------------------------- citas dentro del periodo

    @Test
    void una_ausencia_que_pisa_una_cita_futura_de_la_trabajadora_responde_409_y_no_se_guarda() throws Exception {
        cita(sindi, DIA.withHour(11), EstadoCita.CONFIRMADA);

        var r = ausencia(sindi, DIA.withHour(11).withMinute(30), DIA.withHour(14));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("Hay citas sin cancelar en ese periodo. Muévelas o cancélalas antes.");
        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + sindi.getId(), null))).isEmpty();
    }

    @Test
    void un_bloqueo_global_que_pisa_una_cita_futura_de_cualquier_trabajadora_responde_409_y_no_se_guarda() throws Exception {
        LocalDateTime dia = DIA_GLOBAL_CON_CITA;
        cita(datos.trabajadora("Ana"), dia.withHour(11), EstadoCita.PENDIENTE);

        var r = global(dia, dia.plusDays(1));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("Hay citas sin cancelar en ese periodo. Muévelas o cancélalas antes.");
        assertThat(inicios(admin(HttpMethod.GET, "/api/admin/bloqueos/globales", null))).isEmpty();
    }

    @Test
    void las_citas_canceladas_pasadas_contiguas_o_de_otra_trabajadora_no_impiden_una_ausencia() {
        cita(sindi, DIA.withHour(11), EstadoCita.CANCELADA);
        cita(sindi, DIA.withHour(9), EstadoCita.CONFIRMADA);
        cita(sindi, DIA.withHour(14), EstadoCita.CONFIRMADA);
        cita(datos.trabajadora("Ana"), DIA.withHour(11), EstadoCita.CONFIRMADA);

        assertThat(ausencia(sindi, DIA.withHour(10), DIA.withHour(14)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        LocalDateTime pasado = LocalDateTime.of(2020, 1, 6, 10, 0);
        cita(sindi, pasado.plusHours(1), EstadoCita.CONFIRMADA);
        assertThat(ausencia(sindi, pasado, pasado.plusHours(4)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---------------------------------------------------------------- utilidades

    private void cita(Trabajador t, LocalDateTime inicio, EstadoCita estado) {
        datos.cita(datos.clienta("Laura"), t, datos.servicio("Semipermanente", 60), inicio, estado);
    }

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
