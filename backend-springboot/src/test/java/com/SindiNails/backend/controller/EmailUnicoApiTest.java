package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Un email pertenece a una sola cuenta: registrarse o que la administración ponga un email ya usado responde
 * 409 "El email ya está en uso.", también cuando dos peticiones llegan a la vez (nunca 500).
 *
 * Servidor real (RANDOM_PORT), como el resto de tests HTTP. No son @Transactional: cada test usa emails únicos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EmailUnicoApiTest {

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    @Test
    void la_administracion_no_puede_poner_a_una_clienta_un_email_ya_usado() throws Exception {
        Usuario admin = usuario("Admin", Role.ADMIN);
        Usuario ana = usuario("Ana", Role.USER);
        Usuario marta = usuario("Marta", Role.USER);

        var r = editarComo(admin, ana, marta.getEmail());

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("El email ya está en uso.");
        assertThat(usuarios.findById(ana.getId()).orElseThrow().getEmail()).isEqualTo(ana.getEmail());
    }

    @Test
    void la_administracion_puede_editar_una_clienta_dejandole_su_propio_email() {
        Usuario admin = usuario("Admin", Role.ADMIN);
        Usuario ana = usuario("Ana", Role.USER);

        assertThat(editarComo(admin, ana, ana.getEmail()).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void diez_registros_simultaneos_con_el_mismo_email_crean_una_cuenta_y_el_resto_recibe_409() throws Exception {
        String email = "carrera-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cuerpo = """
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000","password":"secreta1"}
                """.formatted(email);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        ExecutorService hilos = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> respuestas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            respuestas.add(hilos.submit(() -> {
                salida.await();
                return http.exchange("/api/auth/register", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class);
            }));
        }
        salida.countDown();
        TreeMap<Integer, Integer> estados = new TreeMap<>();
        for (Future<ResponseEntity<String>> f : respuestas) {
            ResponseEntity<String> r = f.get(30, TimeUnit.SECONDS);
            estados.merge(r.getStatusCode().value(), 1, Integer::sum);
            if (r.getStatusCode() == HttpStatus.CONFLICT) {
                assertThat(mensaje(r)).isEqualTo("El email ya está en uso.");
            }
        }
        hilos.shutdown();

        assertThat(estados).containsExactly(Map.entry(200, 1), Map.entry(409, 9));
        assertThat(usuarios.findByEmail(email)).isPresent();
    }

    private ResponseEntity<String> editarComo(Usuario admin, Usuario clienta, String email) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange("/api/admin/clientes/" + clienta.getId(), HttpMethod.PUT, new HttpEntity<>("""
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"611111111"}
                """.formatted(email), h), String.class);
    }

    private String mensaje(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("message").asText(null);
    }

    private Usuario usuario(String nombre, Role rol) {
        return usuarios.save(Usuario.builder().nombre(nombre).apellidos("Pérez")
                .email(nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(rol).build());
    }
}
