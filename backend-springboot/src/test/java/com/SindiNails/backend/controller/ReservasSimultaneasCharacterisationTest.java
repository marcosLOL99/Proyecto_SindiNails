package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan lo que pasa HOY con reservas simultáneas del mismo hueco, NO lo que
 * debería pasar. Temporales: se sustituyen al corregir el paso 3.9. Ver la skill characterisation-tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ReservasSimultaneasCharacterisationTest {

    private static final LocalDate DIA = LocalDate.of(2030, 2, 11);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioCita citas;
    @Autowired JwtService jwtService;

    @Test
    void characterises_diez_clientas_reservan_a_la_vez_el_mismo_hueco() throws Exception {
        Trabajador sindi = datos.trabajadora("Sindi");
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        datos.turno(sindi, DIA, "10:00", "14:00");
        String cuerpo = """
                {"idServicio":%d,"idTrabajador":%d,"fechaHoraInicio":"%sT10:00:00"}
                """.formatted(servicio.getId(), sindi.getId(), DIA);

        List<Usuario> clientas = new ArrayList<>();
        for (int i = 0; i < 10; i++) clientas.add(datos.clienta("Clienta" + i));

        ExecutorService hilos = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> estados = new ArrayList<>();
        for (Usuario clienta : clientas) {
            estados.add(hilos.submit(() -> {
                HttpHeaders h = new HttpHeaders();
                h.setContentType(MediaType.APPLICATION_JSON);
                h.setBearerAuth(jwtService.generateToken(clienta));
                salida.await();
                return http.exchange("/api/citas", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class)
                        .getStatusCode().value();
            }));
        }
        salida.countDown();
        TreeMap<Integer, Integer> cuenta = new TreeMap<>();
        for (Future<Integer> f : estados) cuenta.merge(f.get(30, TimeUnit.SECONDS), 1, Integer::sum);
        hilos.shutdown();

        long confirmadas = citas.findCitasSolapadas(sindi.getId(), DIA.atTime(10, 0), DIA.atTime(11, 0)).size();
        // SUSPICIOUS: comprueban el solape a la vez, antes de que ninguna guarde, y quedan confirmadas todas.
        assertThat(cuenta + " confirmadas=" + confirmadas).isEqualTo("{201=10} confirmadas=10");
    }
}
