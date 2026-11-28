package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el comportamiento ACTUAL de la gestión de clientas desde la
 * administración (UsuarioService y /api/admin/clientes), NO lo que debería hacer.
 *
 * Fijan lo que pasa hoy para poder corregir los bugs con TDD y refactorizar con seguridad. Los casos
 * marcados como SUSPICIOUS parecen bugs: se documentan tal cual y se deciden con el usuario.
 *
 * Son temporales: se sustituirán por tests de comportamiento al corregir y refactorizar.
 * Ver la skill characterisation-tests.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus
 * datos con emails únicos, y el listado se comprueba buscando por id (la base de datos es compartida).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ClientasAdminCharacterisationTest {

    private static final String PASSWORD = "secreta1";
    private static final LocalDateTime FUTURO = LocalDateTime.of(2030, 1, 10, 10, 0);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;
    private Usuario ana;

    @BeforeEach
    void unaAdministradoraYUnaClienta() {
        admin = usuario("Admin", Role.ADMIN);
        ana = usuario("Ana", Role.USER);
    }

    // ---------------------------------------------------------------- listar

    @Nested
    class Listar {

        @Test
        void characterises_listado_de_clientas() throws Exception {
            var r = peticion(HttpMethod.GET, "/api/admin/clientes", null);
            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(buscar(r, ana.getId())).isEqualTo("{\"id\":N,\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"email\":\"ana-XXXX@test.com\",\"telefono\":\"600000000\",\"role\":\"USER\"}");
            assertThat(buscar(r, admin.getId())).isEqualTo("ausente");
        }
    }

    // ---------------------------------------------------------------- editar

    @Nested
    class Editar {

        @Test
        void characterises_editar_con_todos_los_campos() throws Exception {
            String email = emailUnico("nueva");
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/" + ana.getId(), """
                    {"nombre":"Anabel","apellidos":"Ruiz","email":"%s","telefono":"611111111","role":"USER"}
                    """.formatted(email));
            assertThat(resumen(r)).isEqualTo("200 {\"id\":N,\"nombre\":\"Anabel\",\"apellidos\":\"Ruiz\",\"email\":\"nueva-XXXX@test.com\",\"telefono\":\"611111111\",\"role\":\"USER\"}");
            assertThat(guardada(ana)).isEqualTo("Anabel|Ruiz|nueva-XXXX@test.com|611111111|USER");
        }

        @Test
        void characterises_editar_a_una_administradora_por_la_ruta_de_clientas() throws Exception {
            Usuario otraAdmin = usuario("Jefa", Role.ADMIN);
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/" + otraAdmin.getId(), """
                    {"nombre":"Cambiada","apellidos":"Salón","email":"%s","telefono":"600000000","role":"USER"}
                    """.formatted(otraAdmin.getEmail()));
            // SUSPICIOUS (C2): la ruta de clientas también edita a una administradora.
            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(guardada(otraAdmin)).isEqualTo("Cambiada|Salón|jefa-XXXX@test.com|600000000|ADMIN");
        }

        @Test
        void characterises_editar_con_email_no_valido() throws Exception {
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/" + ana.getId(), """
                    {"nombre":"Ana","apellidos":"Pérez","email":"no-es-un-email","telefono":"600000000","role":"USER"}
                    """);
            // SUSPICIOUS (C3): solo se valida el teléfono; el registro exige email válido y todos los campos.
            assertThat(resumen(r)).isEqualTo("200 {\"id\":N,\"nombre\":\"Ana\",\"apellidos\":\"Pérez\",\"email\":\"no-es-un-email\",\"telefono\":\"600000000\",\"role\":\"USER\"}");
            assertThat(guardada(ana)).isEqualTo("Ana|Pérez|no-es-un-email|600000000|USER");
        }

        @Test
        void characterises_editar_con_nombre_y_email_vacios() throws Exception {
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/" + ana.getId(), """
                    {"nombre":"","apellidos":"","email":"","telefono":"600000000","role":"USER"}
                    """);
            // SUSPICIOUS (C3): deja a la clienta sin nombre ni email (y sin poder entrar).
            assertThat(resumen(r)).isEqualTo("200 {\"id\":N,\"nombre\":\"\",\"apellidos\":\"\",\"email\":\"\",\"telefono\":\"600000000\",\"role\":\"USER\"}");
            assertThat(guardada(ana)).isEqualTo("|||600000000|USER");
        }

        @Test
        void characterises_editar_una_clienta_que_no_existe() throws Exception {
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/99999999", """
                    {"nombre":"Ana","apellidos":"Pérez","email":"x@test.com","telefono":"600000000","role":"USER"}
                    """);
            // Ya conocido (paso 3.8 del plan): los 404 llegan sin {"message": ...}.
            assertThat(resumen(r)).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/clientes/N\"}");
        }

        @Test
        void characterises_el_id_del_cuerpo_se_ignora() throws Exception {
            Usuario marta = usuario("Marta", Role.USER);
            var r = peticion(HttpMethod.PUT, "/api/admin/clientes/" + ana.getId(), """
                    {"id":%d,"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"622222222","role":"USER"}
                    """.formatted(marta.getId(), ana.getEmail()));
            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(guardada(ana)).isEqualTo("Ana|Pérez|ana-XXXX@test.com|622222222|USER");
            assertThat(guardada(marta)).isEqualTo("Marta|Pérez|marta-XXXX@test.com|600000000|USER");
        }
    }

    // ---------------------------------------------------------------- borrar

    @Nested
    class Borrar {

        @Test
        void characterises_borrar_una_clienta_sin_citas() throws Exception {
            var r = peticion(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);
            assertThat(resumen(r)).isEqualTo("204 null");
            assertThat(usuarios.existsById(ana.getId())).isEqualTo(false);
        }

        @Test
        void characterises_borrar_una_clienta_con_citas() throws Exception {
            var servicio = datos.servicio("Semipermanente", 60);
            var trabajadora = datos.trabajadora("Sindi");
            datos.cita(ana, trabajadora, servicio, FUTURO, EstadoCita.CONFIRMADA);

            var r = peticion(HttpMethod.DELETE, "/api/admin/clientes/" + ana.getId(), null);
            // SUSPICIOUS (C4): la clave foránea de sus citas impide borrarla y llega un 500 sin mensaje.
            assertThat(resumen(r)).isEqualTo("500 {\"timestamp\":T,\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/admin/clientes/N\"}");
            assertThat(usuarios.existsById(ana.getId())).isEqualTo(true);
        }

        @Test
        void characterises_borrar_una_administradora_por_la_ruta_de_clientas() throws Exception {
            Usuario otraAdmin = usuario("Jefa", Role.ADMIN);
            var r = peticion(HttpMethod.DELETE, "/api/admin/clientes/" + otraAdmin.getId(), null);
            // SUSPICIOUS (C2): la ruta de clientas también borra administradoras.
            assertThat(resumen(r)).isEqualTo("204 null");
            assertThat(usuarios.existsById(otraAdmin.getId())).isEqualTo(false);
        }

        @Test
        void characterises_borrarse_a_si_misma() throws Exception {
            var r = peticion(HttpMethod.DELETE, "/api/admin/clientes/" + admin.getId(), null);
            // SUSPICIOUS (C2): una administradora puede borrar su propia cuenta (y dejar el salón sin ninguna).
            assertThat(resumen(r)).isEqualTo("204 null");
            assertThat(usuarios.existsById(admin.getId())).isEqualTo(false);
        }

        @Test
        void characterises_borrar_una_clienta_que_no_existe() throws Exception {
            var r = peticion(HttpMethod.DELETE, "/api/admin/clientes/99999999", null);
            assertThat(resumen(r)).isEqualTo("404 {\"timestamp\":T,\"status\":404,\"error\":\"Not Found\",\"path\":\"/api/admin/clientes/N\"}");
        }
    }

    // ---------------------------------------------------------------- utilidades

    private Usuario usuario(String nombre, Role rol) {
        return usuarios.save(Usuario.builder().nombre(nombre).apellidos("Pérez")
                .email(emailUnico(nombre)).telefono("600000000")
                .password(passwordEncoder.encode(PASSWORD)).role(rol).build());
    }

    private static String emailUnico(String nombre) {
        return nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    /** Lo que hay guardado, en una línea: nombre|apellidos|email|telefono|rol (el email con su id aleatorio enmascarado). */
    private String guardada(Usuario u) {
        Usuario g = usuarios.findById(u.getId()).orElseThrow();
        return String.join("|", g.getNombre(), g.getApellidos(), enmascarar(g.getEmail()), g.getTelefono(),
                String.valueOf(g.getRole()));
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private ResponseEntity<String> sinToken(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    /** Estado y cuerpo en una línea, sin ids ni emails aleatorios. */
    private static String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + enmascarar(String.valueOf(r.getBody()));
    }

    /** La clienta con ese id dentro del listado (JSON, ids y emails enmascarados), o "ausente". */
    private String buscar(ResponseEntity<String> listado, Long id) throws Exception {
        JsonNode lista = json.readTree(listado.getBody());
        return StreamSupport.stream(lista.spliterator(), false)
                .filter(n -> n.path("id").asLong() == id)
                .findFirst()
                .map(n -> enmascarar(n.toString()))
                .orElse("ausente");
    }

    private static String enmascarar(String texto) {
        return texto == null ? "null" : texto
                .replaceAll("-[0-9a-f]{8}@test\\.com", "-XXXX@test.com")
                .replaceAll("\"id\":\\d+", "\"id\":N")
                .replaceAll("/api/admin/clientes/\\d+", "/api/admin/clientes/N")
                .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":T");
    }
}
