package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
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
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La administración ve cada trabajadora con sus datos (id, nombre, apellidos, activo) y sin sus turnos:
 * los turnos se piden aparte en /api/admin/trabajadores/{id}/turnos.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class TrabajadorasAdminApiTest {

    private static final LocalDate DIA = LocalDate.of(2030, 1, 10);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;

    @BeforeEach
    void unaAdministradora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private JsonNode trabajadora(long id, String nombre, String apellidos, boolean activo) throws Exception {
        return json.readTree("""
                {"id":%d,"nombre":"%s","apellidos":"%s","activo":%b}""".formatted(id, nombre, apellidos, activo));
    }

    @Test
    void el_listado_muestra_cada_trabajadora_sin_sus_turnos() throws Exception {
        var sindi = datos.trabajadora("Sindi");
        datos.turno(sindi, DIA, "10:00", "14:00");

        var r = peticion(HttpMethod.GET, "/api/admin/trabajadores", null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deSindi = StreamSupport.stream(json.readTree(r.getBody()).spliterator(), false)
                .filter(t -> t.get("id").asLong() == sindi.getId())
                .findFirst().orElseThrow();
        assertThat(deSindi).isEqualTo(trabajadora(sindi.getId(), "Sindi", "García", true));
    }

    @Test
    void crear_una_trabajadora_devuelve_201_con_sus_datos() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/admin/trabajadores", """
                {"nombre":"Laura","apellidos":"Martín","activo":true}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode cuerpo = json.readTree(r.getBody());
        assertThat(cuerpo).isEqualTo(trabajadora(cuerpo.get("id").asLong(), "Laura", "Martín", true));
    }

    @Test
    void editar_una_trabajadora_con_turnos_devuelve_sus_datos_sin_los_turnos() throws Exception {
        var sindi = datos.trabajadora("Sindi");
        datos.turno(sindi, DIA, "10:00", "14:00");

        var r = peticion(HttpMethod.PUT, "/api/admin/trabajadores/" + sindi.getId(), """
                {"nombre":"Sindy","apellidos":"López","activo":false}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(r.getBody())).isEqualTo(trabajadora(sindi.getId(), "Sindy", "López", false));
    }
}
