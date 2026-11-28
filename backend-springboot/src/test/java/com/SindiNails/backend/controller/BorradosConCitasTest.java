package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioServicios;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lo que tiene citas no se borra: las citas son el historial del salón. Borrar un servicio o una clienta con
 * citas responde 409 con mensaje. Si se borra una clienta justo mientras se le crea una cita, ninguna de las dos
 * peticiones responde 500: la que pierde recibe un error con mensaje.
 *
 * Servidor real (RANDOM_PORT). No son @Transactional: cada test crea sus datos. Días de marzo de 2030 propios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class BorradosConCitasTest {

    private static final LocalDate DIA = LocalDate.of(2030, 3, 11);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioServicios servicios;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;

    @BeforeEach
    void unaAdministradora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
    }

    @Test
    void borrar_un_servicio_con_citas_responde_409_y_no_lo_borra() throws Exception {
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        datos.cita(datos.clienta("Laura"), datos.trabajadora("Sindi"), servicio, LocalDateTime.of(2020, 3, 2, 10, 0),
                EstadoCita.COMPLETADA);

        var r = admin(HttpMethod.DELETE, "/api/admin/servicios/" + servicio.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(json.readTree(r.getBody()).path("message").asText()).isEqualTo("No se puede borrar un servicio con citas.");
        assertThat(servicios.existsById(servicio.getId())).isTrue();
    }

    @Test
    void borrar_un_servicio_sin_citas_lo_borra() {
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);

        var r = admin(HttpMethod.DELETE, "/api/admin/servicios/" + servicio.getId(), null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(servicios.existsById(servicio.getId())).isFalse();
    }

    @Test
    void borrar_una_clienta_mientras_se_le_crea_una_cita_no_responde_500_ninguna_de_las_dos() throws Exception {
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        List<Integer> estados = new ArrayList<>();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 20; i++) {
                Trabajador sindi = datos.trabajadora("Sindi" + i);
                datos.turno(sindi, DIA, "10:00", "14:00");
                Usuario laura = datos.clienta("Laura");
                String cita = """
                        {"idServicio":%d,"idTrabajador":%d,"idCliente":%d,"fechaHoraInicio":"%sT10:00:00"}
                        """.formatted(servicio.getId(), sindi.getId(), laura.getId(), DIA);
                CountDownLatch salida = new CountDownLatch(1);
                Future<ResponseEntity<String>> crear = hilos.submit(() -> {
                    salida.await();
                    return admin(HttpMethod.POST, "/api/admin/citas", cita);
                });
                Future<ResponseEntity<String>> borrar = hilos.submit(() -> {
                    salida.await();
                    return admin(HttpMethod.DELETE, "/api/admin/clientes/" + laura.getId(), null);
                });
                salida.countDown();
                estados.add(crear.get(60, TimeUnit.SECONDS).getStatusCode().value());
                estados.add(borrar.get(60, TimeUnit.SECONDS).getStatusCode().value());
            }
        } finally {
            hilos.shutdownNow();
        }

        assertThat(estados).doesNotContain(500);
    }

    private ResponseEntity<String> admin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }
}
