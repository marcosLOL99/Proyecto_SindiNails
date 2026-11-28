package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gestión de clientas desde la administración (/api/admin/clientes).
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ClientasAdminApiTest {

    private static final String PASSWORD = "secreta1";

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

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

    // ---------------------------------------------------------------- utilidades

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
