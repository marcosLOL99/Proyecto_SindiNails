package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el JSON ACTUAL de los endpoints que devuelven entidades JPA, NO lo que debería devolver.
 *
 * ServicioLocal y TipoServicio salían tal cual por la API (la convención pide DTOs). Estos tests fijan
 * estado y cuerpo para poder pasar a DTOs sabiendo si cambia algo del JSON que recibe el frontend.
 * Son temporales: se sustituirán por tests de comportamiento al terminar ese refactor.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class EntidadesEnLaApiCharacterisationTest {

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

    private ResponseEntity<String> peticion(HttpMethod metodo, String url, Usuario usuario, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) h.setBearerAuth(jwtService.generateToken(usuario));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    /** "estado cuerpo", para fijar ambas cosas a la vez. */
    private static String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + r.getBody();
    }

    // ---------- servicios ----------

    @Test
    void characterises_ver_un_servicio() {
        var s = datos.servicio("Manicura rusa", 60);

        var r = peticion(HttpMethod.GET, "/api/servicios/" + s.getId(), null, null);

        assertThat(resumen(r)).isEqualTo("""
                200 {"id":%d,"nombre":"Manicura rusa","descripcion":"Manicura rusa","precio":20.0,"duracionEnMinutos":60,"tipoServicio":{"id":%d,"nombre":"Manicura"}}"""
                .formatted(s.getId(), s.getTipoServicio().getId()));
    }

    @Test
    void characterises_crear_un_servicio() throws Exception {
        var tipo = datos.servicio("Base", 30).getTipoServicio();

        var r = peticion(HttpMethod.POST, "/api/admin/servicios", admin, """
                {"nombre":"Pedicura","descripcion":"Completa","precio":25.5,"duracionEnMinutos":45,"tipoServicioId":%d}
                """.formatted(tipo.getId()));

        long id = json.readTree(r.getBody()).get("id").asLong();
        assertThat(resumen(r)).isEqualTo("""
                201 {"id":%d,"nombre":"Pedicura","descripcion":"Completa","precio":25.5,"duracionEnMinutos":45,"tipoServicio":{"id":%d,"nombre":"Manicura"}}"""
                .formatted(id, tipo.getId()));
    }

    @Test
    void characterises_editar_un_servicio() {
        var s = datos.servicio("Manicura", 60);

        var r = peticion(HttpMethod.PUT, "/api/admin/servicios/" + s.getId(), admin, """
                {"nombre":"Manicura gel","descripcion":"Con gel","precio":30,"duracionEnMinutos":75,"tipoServicioId":%d}
                """.formatted(s.getTipoServicio().getId()));

        assertThat(resumen(r)).isEqualTo("""
                200 {"id":%d,"nombre":"Manicura gel","descripcion":"Con gel","precio":30.0,"duracionEnMinutos":75,"tipoServicio":{"id":%d,"nombre":"Manicura"}}"""
                .formatted(s.getId(), s.getTipoServicio().getId()));
    }

    @Test
    void characterises_catalogo_filtrado_por_tipo() {
        var s = datos.servicio("Manicura francesa", 45);

        var r = peticion(HttpMethod.GET, "/api/servicios?tipoServicioId=" + s.getTipoServicio().getId(), null, null);

        assertThat(resumen(r)).isEqualTo("""
                200 [{"id":%d,"nombre":"Manicura francesa","descripcion":"Manicura francesa","precio":20.0,"duracionEnMinutos":45,"tipoServicio":{"id":%d,"nombre":"Manicura"}}]"""
                .formatted(s.getId(), s.getTipoServicio().getId()));
    }

    @Test
    void characterises_catalogo_completo() {
        var s = datos.servicio("Esmaltado", 30);

        var r = peticion(HttpMethod.GET, "/api/servicios", null, null);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).startsWith("[").contains("""
                {"id":%d,"nombre":"Esmaltado","descripcion":"Esmaltado","precio":20.0,"duracionEnMinutos":30,"tipoServicio":{"id":%d,"nombre":"Manicura"}}"""
                .formatted(s.getId(), s.getTipoServicio().getId()));
    }

    @Test
    void characterises_tipos_de_servicio() {
        var tipo = datos.servicio("Uñas acrílicas", 90).getTipoServicio();

        var r = peticion(HttpMethod.GET, "/api/tiposervicios", null, null);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).startsWith("[").contains("""
                {"id":%d,"nombre":"Manicura"}""".formatted(tipo.getId()));
    }
}
