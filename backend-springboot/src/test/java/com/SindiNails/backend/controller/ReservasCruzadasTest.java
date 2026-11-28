package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioTurno;
import com.SindiNails.backend.repository.RepositorioUsuario;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Una reserva que se cruza con un cambio de horario de la misma trabajadora (borrar su turno, rehacer su
 * semana o crear una ausencia): gana una de las dos, nunca las dos. Si ganaran las dos, quedaría una cita
 * confirmada fuera de turno o dentro de una ausencia.
 *
 * Una carrera no se da en todos los intentos, así que cada test la repite en varias rondas, cada una con su
 * trabajadora. Servidor real (RANDOM_PORT). Días de febrero de 2030 propios de este test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ReservasCruzadasTest {

    private static final int RONDAS = 20;
    private static final LocalDate LUNES = LocalDate.of(2030, 2, 18);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired RepositorioTurno turnos;
    @Autowired JwtService jwtService;

    @Test
    void una_reserva_y_el_borrado_de_su_turno_a_la_vez_no_salen_bien_las_dos() throws Exception {
        assertThat(rondas(sindi -> {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            Long turnoId = turnos.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(sindi.getId(), LUNES).get(0).getId();
            return () -> admin(HttpMethod.DELETE, "/api/admin/turnos/" + turnoId, null);
        })).doesNotContain("las dos");
    }

    @Test
    void una_reserva_y_rehacer_la_semana_sin_ese_dia_a_la_vez_no_salen_bien_las_dos() throws Exception {
        assertThat(rondas(sindi -> {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            return () -> admin(HttpMethod.POST, "/api/admin/turnos/batch", """
                    {"trabajadorId":%d,"fechaInicioSemana":"%s","horarios":{"0":{"activo":false}}}
                    """.formatted(sindi.getId(), LUNES));
        })).doesNotContain("las dos");
    }

    @Test
    void una_reserva_y_una_ausencia_en_esa_hora_a_la_vez_no_salen_bien_las_dos() throws Exception {
        assertThat(rondas(sindi -> {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            return () -> admin(HttpMethod.POST, "/api/admin/bloqueos", """
                    {"fechaHoraInicio":"%sT10:00:00","fechaHoraFin":"%sT12:00:00","motivo":"Médico",
                     "tipo":"TRABAJADOR","trabajadorId":%d}
                    """.formatted(LUNES, LUNES, sindi.getId()));
        })).doesNotContain("las dos");
    }

    @Test
    void una_reserva_y_acortar_su_turno_a_la_vez_no_salen_bien_las_dos() throws Exception {
        assertThat(rondas(sindi -> {
            datos.turno(sindi, LUNES, "10:00", "14:00");
            Long turnoId = turnos.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(sindi.getId(), LUNES).get(0).getId();
            return () -> admin(HttpMethod.PUT, "/api/admin/turnos/" + turnoId, """
                    {"trabajadorId":%d,"fecha":"%s","horaInicio":"12:00","horaFin":"14:00"}
                    """.formatted(sindi.getId(), LUNES));
        })).doesNotContain("las dos");
    }

    @Test
    void dos_turnos_solapados_creados_a_la_vez_no_se_guardan_los_dos() throws Exception {
        nuevaAdministradora();
        List<String> resultados = new ArrayList<>();
        for (int i = 0; i < RONDAS; i++) {
            Trabajador sindi = datos.trabajadora("Sindi" + i);
            var r = aLaVez(
                    () -> admin(HttpMethod.POST, "/api/admin/turnos", turnoJson(sindi, LUNES, "09:00", "14:00")),
                    () -> admin(HttpMethod.POST, "/api/admin/turnos", turnoJson(sindi, LUNES, "12:00", "18:00")));
            resultados.add(r.get(0).getStatusCode().is2xxSuccessful() && r.get(1).getStatusCode().is2xxSuccessful()
                    ? "los dos" : "uno");
        }
        assertThat(resultados).doesNotContain("los dos");
    }

    @Test
    void mover_un_turno_a_otra_trabajadora_y_borrarlo_a_la_vez_no_deja_su_cita_fuera_de_turno() throws Exception {
        nuevaAdministradora();
        servicio = datos.servicio("Semipermanente", 60);
        LocalDate martes = LUNES.plusDays(1);
        List<String> resultados = new ArrayList<>();
        for (int i = 0; i < RONDAS; i++) {
            Trabajador ana = datos.trabajadora("Ana" + i);
            Trabajador bea = datos.trabajadora("Bea" + i);
            datos.turno(ana, LUNES, "10:00", "14:00");
            Long turnoId = turnos.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(ana.getId(), LUNES).get(0).getId();
            // Una cita de Bea el martes que solo queda cubierta si el turno pasa a ser suyo ese día.
            datos.cita(datos.clienta("Laura"), bea, servicio, martes.atTime(11, 0), EstadoCita.CONFIRMADA);

            var r = aLaVez(
                    () -> admin(HttpMethod.PUT, "/api/admin/turnos/" + turnoId, turnoJson(bea, martes, "10:00", "14:00")),
                    () -> admin(HttpMethod.DELETE, "/api/admin/turnos/" + turnoId, null));
            resultados.add(r.get(0).getStatusCode().is2xxSuccessful() && r.get(1).getStatusCode().is2xxSuccessful()
                    ? "las dos" : "una");
        }
        // Si el turno pasa primero a Bea, borrarlo dejaría su cita fuera de turno: el borrado se rechaza (409).
        // Si el borrado llega primero, el turno ya no existe y moverlo da 404. Las dos bien = cita fuera de turno.
        assertThat(resultados).doesNotContain("las dos");
    }

    // ---------------------------------------------------------------- utilidades

    private Usuario admin;
    private ServicioLocal servicio;

    private void nuevaAdministradora() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
    }

    private static String turnoJson(Trabajador t, LocalDate fecha, String inicio, String fin) {
        return """
                {"trabajadorId":%d,"fecha":"%s","horaInicio":"%s","horaFin":"%s"}
                """.formatted(t.getId(), fecha, inicio, fin);
    }

    /** Lanza las dos peticiones a la vez y devuelve sus respuestas en el mismo orden. */
    private static List<ResponseEntity<String>> aLaVez(Callable<ResponseEntity<String>> a,
                                                        Callable<ResponseEntity<String>> b) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch salida = new CountDownLatch(1);
            Future<ResponseEntity<String>> fa = hilos.submit(() -> { salida.await(); return a.call(); });
            Future<ResponseEntity<String>> fb = hilos.submit(() -> { salida.await(); return b.call(); });
            salida.countDown();
            return List.of(fa.get(60, TimeUnit.SECONDS), fb.get(60, TimeUnit.SECONDS));
        } finally {
            hilos.shutdownNow();
        }
    }

    /**
     * En cada ronda, una trabajadora nueva: se prepara su horario y, a la vez, una clienta reserva a las 10:00
     * y la administración hace el cambio (la función devuelve la petición del cambio, sin lanzarla todavía).
     * Devuelve el resultado de cada ronda: "reserva", "cambio" o "las dos" (las dos acabaron bien).
     */
    private List<String> rondas(Function<Trabajador, Callable<ResponseEntity<String>>> prepararCambio) throws Exception {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        servicio = datos.servicio("Semipermanente", 60);
        List<String> resultados = new ArrayList<>();
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < RONDAS; i++) {
                Trabajador sindi = datos.trabajadora("Sindi" + i);
                Usuario clienta = datos.clienta("Laura");
                Callable<ResponseEntity<String>> cambio = prepararCambio.apply(sindi);
                CountDownLatch salida = new CountDownLatch(1);
                Future<ResponseEntity<String>> reserva = hilos.submit(() -> {
                    salida.await();
                    return reservar(clienta, sindi);
                });
                Future<ResponseEntity<String>> delAdmin = hilos.submit(() -> {
                    salida.await();
                    return cambio.call();
                });
                salida.countDown();
                boolean reservaBien = reserva.get(60, TimeUnit.SECONDS).getStatusCode().is2xxSuccessful();
                boolean cambioBien = delAdmin.get(60, TimeUnit.SECONDS).getStatusCode().is2xxSuccessful();
                resultados.add(reservaBien && cambioBien ? "las dos" : reservaBien ? "reserva" : cambioBien ? "cambio" : "ninguna");
            }
        } finally {
            hilos.shutdownNow();
        }
        return resultados;
    }

    private ResponseEntity<String> reservar(Usuario clienta, Trabajador t) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(clienta));
        return http.exchange("/api/citas", HttpMethod.POST, new HttpEntity<>("""
                {"idServicio":%d,"idTrabajador":%d,"fechaHoraInicio":"%sT10:00:00"}
                """.formatted(servicio.getId(), t.getId(), LUNES), h), String.class);
    }

    private ResponseEntity<String> admin(HttpMethod metodo, String url, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(jwtService.generateToken(admin));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }
}
