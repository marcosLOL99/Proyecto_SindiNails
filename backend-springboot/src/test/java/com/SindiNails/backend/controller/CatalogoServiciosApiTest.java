package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * El catálogo de servicios: la administración puede cambiar un servicio de tipo, y pedir un servicio
 * que no existe responde 404.
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test crea sus datos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class CatalogoServiciosApiTest {

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    @Test
    void la_administracion_puede_cambiar_un_servicio_de_tipo() throws Exception {
        var servicio = datos.servicio("Manicura", 60);
        var otroTipo = datos.servicio("Pedicura", 45).getTipoServicio();
        Usuario admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));

        var r = http.exchange("/api/admin/servicios/" + servicio.getId(), HttpMethod.PUT, new HttpEntity<>("""
                {"nombre":"Manicura","precio":20,"duracionEnMinutos":60,"tipoServicioId":%d}
                """.formatted(otroTipo.getId()), h), String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(json.readTree(r.getBody()).at("/tipoServicio/id").asLong()).isEqualTo(otroTipo.getId());
        var guardado = http.getForEntity("/api/servicios/" + servicio.getId(), String.class);
        assertThat(json.readTree(guardado.getBody()).at("/tipoServicio/id").asLong()).isEqualTo(otroTipo.getId());
    }

    @Test
    void pedir_un_servicio_que_no_existe_responde_404() {
        var r = http.getForEntity("/api/servicios/999999999", String.class);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
