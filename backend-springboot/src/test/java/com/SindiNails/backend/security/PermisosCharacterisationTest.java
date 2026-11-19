package com.SindiNails.backend.security;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * CHARACTERISATION TESTS -- documentan los permisos ACTUALES de cada endpoint, NO los que debería tener.
 *
 * Para cada endpoint y cada tipo de usuario (anónimo, USER, ADMIN) fijan si Spring Security deja pasar
 * la petición al controlador ("pasa") o la corta antes ("bloquea <estado>"). Lo que haga después el
 * controlador (200, 400 por datos vacíos, 404 por un id que no existe...) no importa aquí.
 *
 * Usan MockMvc a propósito: sin la redirección a /error de Tomcat, "llega al controlador" se distingue
 * sin ambigüedad (MvcResult.getHandler() no es null, o el controlador lanza una excepción).
 * En AuthCharacterisationTest está lo que recibe de verdad el cliente (servidor real).
 *
 * Son temporales: se sustituirán por tests de comportamiento al corregir la seguridad (Fase 2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class PermisosCharacterisationTest {

    private static final String ID = "999999";

    @Autowired MockMvc mvc;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;

    private record Endpoint(HttpMethod metodo, String url) {
        @Override public String toString() { return metodo + " " + url; }
    }

    private static Endpoint e(HttpMethod metodo, String url) {
        return new Endpoint(metodo, url);
    }

    private static final List<Endpoint> ENDPOINTS = List.of(
            // auth y usuario
            e(HttpMethod.POST, "/api/auth/register"),
            e(HttpMethod.POST, "/api/auth/login"),
            e(HttpMethod.GET, "/api/users/me"),
            e(HttpMethod.PUT, "/api/users/me"),
            // catálogo público
            e(HttpMethod.GET, "/api/servicios"),
            e(HttpMethod.GET, "/api/servicios/" + ID),
            e(HttpMethod.GET, "/api/tiposervicios"),
            e(HttpMethod.GET, "/api/trabajadores"),
            // citas de la clienta
            e(HttpMethod.GET, "/api/citas/disponibilidad"),
            e(HttpMethod.GET, "/api/citas/dias-no-disponibles"),
            e(HttpMethod.POST, "/api/citas"),
            e(HttpMethod.GET, "/api/citas/mis-citas"),
            e(HttpMethod.PATCH, "/api/citas/" + ID + "/cancelar"),
            // administración
            e(HttpMethod.GET, "/api/admin/citas"),
            e(HttpMethod.POST, "/api/admin/citas"),
            e(HttpMethod.GET, "/api/admin/stats/citas-semanales"),
            e(HttpMethod.PUT, "/api/admin/citas/" + ID),
            e(HttpMethod.DELETE, "/api/admin/citas/" + ID),
            e(HttpMethod.POST, "/api/admin/servicios"),
            e(HttpMethod.PUT, "/api/admin/servicios/" + ID),
            e(HttpMethod.DELETE, "/api/admin/servicios/" + ID),
            e(HttpMethod.GET, "/api/admin/trabajadores"),
            e(HttpMethod.POST, "/api/admin/trabajadores"),
            e(HttpMethod.PUT, "/api/admin/trabajadores/" + ID),
            e(HttpMethod.DELETE, "/api/admin/trabajadores/" + ID),
            e(HttpMethod.GET, "/api/admin/trabajadores/" + ID + "/turnos"),
            e(HttpMethod.POST, "/api/admin/turnos"),
            e(HttpMethod.POST, "/api/admin/turnos/batch"),
            e(HttpMethod.PUT, "/api/admin/turnos/" + ID),
            e(HttpMethod.DELETE, "/api/admin/turnos/" + ID),
            e(HttpMethod.GET, "/api/admin/clientes"),
            e(HttpMethod.PUT, "/api/admin/clientes/" + ID),
            e(HttpMethod.DELETE, "/api/admin/clientes/" + ID),
            e(HttpMethod.POST, "/api/admin/bloqueos"),
            e(HttpMethod.GET, "/api/admin/bloqueos/trabajador/" + ID),
            e(HttpMethod.GET, "/api/admin/bloqueos/globales"),
            e(HttpMethod.DELETE, "/api/admin/bloqueos/" + ID)
    );

    private String tokenDe(Role role) {
        Usuario u = usuarios.save(Usuario.builder().nombre("Ana").apellidos("Pérez")
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(role).build());
        return jwtService.generateToken(u);
    }

    /** "pasa" si la petición llega al controlador; "bloquea <estado>" si la corta Spring Security. */
    private String acceso(Endpoint endpoint, String token) {
        MockHttpServletRequestBuilder peticion = request(endpoint.metodo(), endpoint.url());
        if (endpoint.metodo() != HttpMethod.GET && endpoint.metodo() != HttpMethod.DELETE) {
            peticion.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        if (token != null) peticion.header("Authorization", "Bearer " + token);
        try {
            MvcResult r = mvc.perform(peticion).andReturn();
            return r.getHandler() != null ? "pasa" : "bloquea " + r.getResponse().getStatus();
        } catch (Exception excepcionDelControlador) {
            return "pasa";
        }
    }

    @Test
    void characterises_quien_llega_a_cada_endpoint() {
        String tokenUser = tokenDe(Role.USER);
        String tokenAdmin = tokenDe(Role.ADMIN);

        StringBuilder matriz = new StringBuilder();
        for (Endpoint endpoint : ENDPOINTS) {
            matriz.append("%-45s | anónimo: %-11s | USER: %-11s | ADMIN: %s%n".formatted(endpoint,
                    acceso(endpoint, null), acceso(endpoint, tokenUser), acceso(endpoint, tokenAdmin)));
        }

        assertThat(matriz.toString()).isEqualToNormalizingNewlines("""
                POST /api/auth/register                       | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                POST /api/auth/login                          | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/users/me                             | anónimo: bloquea 403 | USER: pasa        | ADMIN: pasa
                PUT /api/users/me                             | anónimo: bloquea 403 | USER: pasa        | ADMIN: pasa
                GET /api/servicios                            | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/servicios/999999                     | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/tiposervicios                        | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/trabajadores                         | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/citas/disponibilidad                 | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                GET /api/citas/dias-no-disponibles            | anónimo: pasa        | USER: pasa        | ADMIN: pasa
                POST /api/citas                               | anónimo: bloquea 403 | USER: pasa        | ADMIN: pasa
                GET /api/citas/mis-citas                      | anónimo: bloquea 403 | USER: pasa        | ADMIN: pasa
                PATCH /api/citas/999999/cancelar              | anónimo: bloquea 403 | USER: pasa        | ADMIN: pasa
                GET /api/admin/citas                          | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/citas                         | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/stats/citas-semanales          | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                PUT /api/admin/citas/999999                   | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/citas/999999                | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/servicios                     | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                PUT /api/admin/servicios/999999               | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/servicios/999999            | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/trabajadores                   | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/trabajadores                  | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                PUT /api/admin/trabajadores/999999            | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/trabajadores/999999         | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/trabajadores/999999/turnos     | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/turnos                        | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/turnos/batch                  | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                PUT /api/admin/turnos/999999                  | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/turnos/999999               | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/clientes                       | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                PUT /api/admin/clientes/999999                | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/clientes/999999             | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                POST /api/admin/bloqueos                      | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/bloqueos/trabajador/999999     | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                GET /api/admin/bloqueos/globales              | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                DELETE /api/admin/bloqueos/999999             | anónimo: bloquea 403 | USER: bloquea 403 | ADMIN: pasa
                """);
    }
}
