package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan lo que pasa HOY cuando dos cuentas acabarían con el mismo email,
 * NO lo que debería pasar. Temporales: se sustituyen al corregir el paso 3.7. Ver la skill characterisation-tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class EmailDuplicadoCharacterisationTest {

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;

    @Test
    void characterises_la_administracion_pone_a_una_clienta_un_email_ya_usado() {
        Usuario admin = usuario("Admin", Role.ADMIN);
        Usuario ana = usuario("Ana", Role.USER);
        Usuario marta = usuario("Marta", Role.USER);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));

        var r = http.exchange("/api/admin/clientes/" + ana.getId(), HttpMethod.PUT, new HttpEntity<>("""
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000"}
                """.formatted(marta.getEmail()), h), String.class);

        // SUSPICIOUS: llega 500 sin mensaje en vez de 409 "El email ya está en uso.".
        assertThat(r.getStatusCode().value() + " " + r.getBody().replaceAll("\"timestamp\":\"[^\"]+\"", "T")
                .replaceAll("/\\d+", "/N")).isEqualTo("500 {T,\"status\":500,\"error\":\"Internal Server Error\",\"path\":\"/api/admin/clientes/N\"}");
    }

    @Test
    void characterises_diez_registros_simultaneos_con_el_mismo_email() throws Exception {
        String email = "carrera-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String cuerpo = """
                {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000","password":"secreta1"}
                """.formatted(email);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        ExecutorService hilos = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> estados = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            estados.add(hilos.submit(() -> {
                salida.await();
                return http.exchange("/api/auth/register", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class)
                        .getStatusCode().value();
            }));
        }
        salida.countDown();
        TreeMap<Integer, Integer> cuenta = new TreeMap<>();
        for (Future<Integer> f : estados) cuenta.merge(f.get(30, TimeUnit.SECONDS), 1, Integer::sum);
        hilos.shutdown();

        // SUSPICIOUS: la restricción unique impide el duplicado, pero los que pierden la carrera reciben 500.
        assertThat(cuenta.toString()).isEqualTo("{200=1, 500=9}");
        assertThat(usuarios.findAll().stream().filter(u -> u.getEmail().equals(email)).count()).isEqualTo(1L);
    }

    private Usuario usuario(String nombre, Role rol) {
        return usuarios.save(Usuario.builder().nombre(nombre).apellidos("Pérez")
                .email(nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(rol).build());
    }
}
