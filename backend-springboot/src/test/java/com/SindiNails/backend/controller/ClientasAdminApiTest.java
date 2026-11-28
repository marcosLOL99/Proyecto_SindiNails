package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gestión de clientas desde la administración (/api/admin/clientes).
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ClientasAdminApiTest {

    private static final String PASSWORD = "secreta1";

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioCita citas;
    @Autowired ObjectMapper json;

    private Usuario admin;
    private Usuario ana;

    @BeforeEach
    void unaAdministradoraYUnaClienta() {
        admin = usuario("Admin", Role.ADMIN);
        ana = usuario("Ana", Role.USER);
    }

    // ---------------------------------------------------------------- C1: el rol no se cambia al editar

    @Test
    void editar_una_clienta_sin_enviar_el_rol_la_deja_como_clienta_y_puede_seguir_entrando() {
        var r = editar(ana, """
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"611111111"}
                """.formatted(ana.getEmail()));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rolGuardado(ana)).isEqualTo(Role.USER);
        assertThat(login(ana).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void editar_una_clienta_no_la_convierte_en_administradora() {
        var r = editar(ana, """
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000","role":"ADMIN"}
                """.formatted(ana.getEmail()));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rolGuardado(ana)).isEqualTo(Role.USER);
    }

    // ---------------------------------------------------------------- C2: solo clientas

    @Test
    void editar_una_administradora_por_la_ruta_de_clientas_responde_404_y_no_la_cambia() {
        Usuario jefa = usuario("Jefa", Role.ADMIN);

        var r = editar(jefa, """
                {"nombre":"Cambiada","apellidos":"Salón","email":"%s","telefono":"611111111","role":"USER"}
                """.formatted(jefa.getEmail()));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Usuario guardada = usuarios.findById(jefa.getId()).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Jefa");
        assertThat(guardada.getTelefono()).isEqualTo("600000000");
    }

    @Test
    void borrar_una_administradora_por_la_ruta_de_clientas_responde_404_y_no_la_borra() {
        Usuario jefa = usuario("Jefa", Role.ADMIN);

        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + jefa.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(usuarios.existsById(jefa.getId())).isTrue();
    }

    @Test
    void una_administradora_no_puede_borrar_su_propia_cuenta_por_la_ruta_de_clientas() {
        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + admin.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(usuarios.existsById(admin.getId())).isTrue();
    }

    @Test
    void borrar_una_clienta_sin_citas_la_borra() {
        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarios.existsById(ana.getId())).isFalse();
    }

    // ---------------------------------------------------------------- C3: mismas reglas que el registro

    @Test
    void editar_una_clienta_con_nombre_apellidos_y_email_vacios_responde_400_y_no_cambia_nada() throws Exception {
        var r = editar(ana, """
                {"nombre":" ","apellidos":"","email":"","telefono":"611111111"}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("Los apellidos son obligatorios. El email es obligatorio. "
                + "El nombre es obligatorio.");
        assertThat(sinCambios(ana)).isTrue();
    }

    @Test
    void editar_una_clienta_con_un_email_no_valido_responde_400_y_no_cambia_nada() throws Exception {
        var r = editar(ana, """
                {"nombre":"Ana","apellidos":"Pérez","email":"no-es-un-email","telefono":"611111111"}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("El email no es válido.");
        assertThat(sinCambios(ana)).isTrue();
    }

    @Test
    void editar_una_clienta_con_datos_validos_guarda_los_cambios() {
        String nuevoEmail = "anabel-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        var r = editar(ana, """
                {"nombre":"Anabel","apellidos":"Ruiz","email":"%s","telefono":"611111111"}
                """.formatted(nuevoEmail));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        Usuario guardada = usuarios.findById(ana.getId()).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Anabel");
        assertThat(guardada.getApellidos()).isEqualTo("Ruiz");
        assertThat(guardada.getEmail()).isEqualTo(nuevoEmail);
        assertThat(guardada.getTelefono()).isEqualTo("611111111");
    }

    // ---------------------------------------------------------------- C4: clientas con citas

    @Test
    void borrar_una_clienta_con_citas_responde_409_y_no_borra_nada() throws Exception {
        var cita = datos.cita(ana, datos.trabajadora("Sindi"), datos.servicio("Semipermanente", 60),
                LocalDateTime.of(2020, 3, 2, 10, 0), EstadoCita.CONFIRMADA);

        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("No se puede borrar una clienta con citas.");
        assertThat(usuarios.existsById(ana.getId())).isTrue();
        assertThat(citas.existsById(cita.getId())).isTrue();
    }

    @Test
    void borrar_una_clienta_con_solo_citas_canceladas_tampoco_se_permite() throws Exception {
        datos.cita(ana, datos.trabajadora("Sindi"), datos.servicio("Semipermanente", 60),
                LocalDateTime.of(2030, 3, 2, 10, 0), EstadoCita.CANCELADA);

        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(usuarios.existsById(ana.getId())).isTrue();
    }

    @Test
    void las_citas_de_otra_clienta_no_impiden_borrar() {
        Usuario marta = usuario("Marta", Role.USER);
        datos.cita(marta, datos.trabajadora("Sindi"), datos.servicio("Semipermanente", 60),
                LocalDateTime.of(2030, 3, 2, 10, 0), EstadoCita.CONFIRMADA);

        var r = comoAdmin(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarios.existsById(ana.getId())).isFalse();
    }

    // ---------------------------------------------------------------- utilidades

    private boolean sinCambios(Usuario u) {
        Usuario g = usuarios.findById(u.getId()).orElseThrow();
        return g.getNombre().equals(u.getNombre()) && g.getApellidos().equals(u.getApellidos())
                && g.getEmail().equals(u.getEmail()) && g.getTelefono().equals(u.getTelefono());
    }

    private String mensaje(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("message").asText(null);
    }

    private Usuario usuario(String nombre, Role rol) {
        return usuarios.save(Usuario.builder().nombre(nombre).apellidos("Pérez")
                .email(nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password(passwordEncoder.encode(PASSWORD)).role(rol).build());
    }

    private Role rolGuardado(Usuario u) {
        return usuarios.findById(u.getId()).orElseThrow().getRole();
    }

    private ResponseEntity<String> editar(Usuario u, String cuerpo) {
        return comoAdmin(HttpMethod.PUT, "/api/admin/clientes/" + u.getId(), cuerpo);
    }

    private ResponseEntity<String> comoAdmin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private ResponseEntity<String> login(Usuario u) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String cuerpo = """
                {"email":"%s","password":"%s"}
                """.formatted(usuarios.findById(u.getId()).orElseThrow().getEmail(), PASSWORD);
        return http.exchange("/api/auth/login", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class);
    }
}
