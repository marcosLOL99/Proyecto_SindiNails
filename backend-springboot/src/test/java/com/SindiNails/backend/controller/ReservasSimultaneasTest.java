package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
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

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reservas y cambios de cita que llegan a la vez: dos citas sin cancelar de la misma trabajadora nunca
 * se solapan, aunque las peticiones se crucen. La que llega tarde recibe 409 con el mensaje de siempre.
 *
 * Servidor real (RANDOM_PORT) y peticiones lanzadas a la vez desde varios hilos. No son @Transactional:
 * cada test crea sus trabajadoras. Días de febrero de 2030 propios de este test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ReservasSimultaneasTest {

    private static final LocalDate DIA = LocalDate.of(2030, 2, 12);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioCita citas;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    @Test
    void de_diez_clientas_que_reservan_a_la_vez_el_mismo_hueco_solo_una_lo_consigue() throws Exception {
        Trabajador sindi = datos.trabajadora("Sindi");
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        datos.turno(sindi, DIA, "10:00", "14:00");
        List<Callable<ResponseEntity<String>>> reservas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Usuario clienta = datos.clienta("Clienta" + i);
            reservas.add(() -> reservar(clienta, servicio, sindi, "10:00"));
        }

        List<ResponseEntity<String>> respuestas = aLaVez(reservas);

        assertThat(estados(respuestas)).containsExactly(Map.entry(201, 1), Map.entry(409, 9));
        for (var r : respuestas) {
            if (r.getStatusCode() == HttpStatus.CONFLICT) {
                assertThat(mensaje(r)).isEqualTo("El trabajador no está disponible en el horario seleccionado.");
            }
        }
        assertThat(citas.findCitasSolapadas(sindi.getId(), DIA.atTime(10, 0), DIA.atTime(11, 0))).hasSize(1);
    }

    @Test
    void reservas_a_la_vez_que_se_solapan_en_parte_dejan_como_mucho_una_por_franja() throws Exception {
        Trabajador sindi = datos.trabajadora("Sindi");
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        datos.turno(sindi, DIA, "10:00", "14:00");
        List<Callable<ResponseEntity<String>>> reservas = new ArrayList<>();
        for (String hora : List.of("10:00", "10:15", "10:30", "10:45", "11:00", "10:00", "10:30", "11:00")) {
            Usuario clienta = datos.clienta("Clienta");
            reservas.add(() -> reservar(clienta, servicio, sindi, hora));
        }

        aLaVez(reservas);

        List<Cita> confirmadas = citas.findCitasSolapadas(sindi.getId(), DIA.atTime(0, 0), DIA.atTime(23, 59));
        for (Cita a : confirmadas) {
            for (Cita b : confirmadas) {
                if (!a.getId().equals(b.getId())) {
                    boolean seSolapan = a.getFechaHoraInicio().isBefore(b.getFechaHoraFin())
                            && b.getFechaHoraInicio().isBefore(a.getFechaHoraFin());
                    assertThat(seSolapan).as(a.getFechaHoraInicio() + " y " + b.getFechaHoraInicio()).isFalse();
                }
            }
        }
        assertThat(confirmadas).isNotEmpty();
    }

    @Test
    void si_la_administracion_mueve_a_la_vez_dos_citas_al_mismo_hueco_solo_una_se_mueve() throws Exception {
        Usuario admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        Trabajador sindi = datos.trabajadora("Sindi");
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        datos.turno(sindi, DIA, "10:00", "14:00");
        Cita once = datos.cita(datos.clienta("Laura"), sindi, servicio, DIA.atTime(11, 0), EstadoCita.CONFIRMADA);
        Cita doce = datos.cita(datos.clienta("Marta"), sindi, servicio, DIA.atTime(12, 0), EstadoCita.CONFIRMADA);

        List<ResponseEntity<String>> respuestas = aLaVez(List.of(
                () -> mover(admin, once, servicio, sindi, "10:00"),
                () -> mover(admin, doce, servicio, sindi, "10:00")));

        assertThat(estados(respuestas)).containsExactly(Map.entry(200, 1), Map.entry(409, 1));
        assertThat(citas.findCitasSolapadas(sindi.getId(), DIA.atTime(10, 0), DIA.atTime(11, 0))).hasSize(1);
    }

    @Test
    void reservas_a_la_vez_con_trabajadoras_distintas_salen_todas() throws Exception {
        ServicioLocal servicio = datos.servicio("Semipermanente", 60);
        List<Callable<ResponseEntity<String>>> reservas = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Trabajador t = datos.trabajadora("Trabajadora" + i);
            datos.turno(t, DIA, "10:00", "14:00");
            Usuario clienta = datos.clienta("Clienta" + i);
            reservas.add(() -> reservar(clienta, servicio, t, "10:00"));
        }

        assertThat(estados(aLaVez(reservas))).containsExactly(Map.entry(201, 5));
    }

    // ---------------------------------------------------------------- utilidades

    /** Lanza todas las peticiones a la vez (cada una en su hilo, con una salida común) y espera sus respuestas. */
    private static List<ResponseEntity<String>> aLaVez(List<Callable<ResponseEntity<String>>> peticiones) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(peticiones.size());
        try {
            CountDownLatch salida = new CountDownLatch(1);
            List<Future<ResponseEntity<String>>> futuras = new ArrayList<>();
            for (Callable<ResponseEntity<String>> p : peticiones) {
                futuras.add(hilos.submit(() -> {
                    salida.await();
                    return p.call();
                }));
            }
            salida.countDown();
            List<ResponseEntity<String>> respuestas = new ArrayList<>();
            for (Future<ResponseEntity<String>> f : futuras) respuestas.add(f.get(60, TimeUnit.SECONDS));
            return respuestas;
        } finally {
            hilos.shutdownNow();
        }
    }

    private static TreeMap<Integer, Integer> estados(List<ResponseEntity<String>> respuestas) {
        TreeMap<Integer, Integer> cuenta = new TreeMap<>();
        respuestas.forEach(r -> cuenta.merge(r.getStatusCode().value(), 1, Integer::sum));
        return cuenta;
    }

    private ResponseEntity<String> reservar(Usuario clienta, ServicioLocal servicio, Trabajador t, String hora) {
        return http.exchange("/api/citas", HttpMethod.POST, new HttpEntity<>(citaJson(servicio, t, hora), conToken(clienta)),
                String.class);
    }

    private ResponseEntity<String> mover(Usuario admin, Cita cita, ServicioLocal servicio, Trabajador t, String hora) {
        return http.exchange("/api/admin/citas/" + cita.getId(), HttpMethod.PUT,
                new HttpEntity<>(citaJson(servicio, t, hora), conToken(admin)), String.class);
    }

    private static String citaJson(ServicioLocal servicio, Trabajador t, String hora) {
        return """
                {"idServicio":%d,"idTrabajador":%d,"fechaHoraInicio":"%sT%s:00"}
                """.formatted(servicio.getId(), t.getId(), DIA, hora);
    }

    private HttpHeaders conToken(Usuario usuario) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(usuario));
        return h;
    }

    private String mensaje(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("message").asText(null);
    }
}
