package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el JSON ACTUAL de los listados de citas y de la disponibilidad
 * (/api/citas/mis-citas, /api/admin/citas, /api/admin/stats/citas-semanales, /api/citas/disponibilidad,
 * /api/citas/dias-no-disponibles), NO lo que debería devolver.
 *
 * Fijan la forma de las respuestas (campos, formato de fechas, orden, quién ve qué, errores de los parámetros)
 * para refactorizar CitaService con seguridad. Las reglas de los huecos ya están en CitaServiceTest.
 * Los casos marcados como SUSPICIOUS parecen bugs: se documentan tal cual y se deciden con el usuario.
 *
 * Son temporales: se sustituirán por tests de comportamiento al refactorizar. Ver la skill characterisation-tests.
 *
 * Servidor real (RANDOM_PORT). No son @Transactional: cada test crea su trabajadora y sus clientas, y los
 * listados compartidos se filtran por la trabajadora del test. Fechas en febrero de 2030, que ningún bloqueo
 * global de otros tests ocupa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class CitasListadosCharacterisationTest {

    private static final LocalDate LUNES = LocalDate.of(2030, 2, 4);

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario admin;
    private Usuario laura;
    private Usuario marta;
    private Trabajador sindi;
    private ServicioLocal servicio;

    @BeforeEach
    void datosBase() {
        admin = usuarios.save(Usuario.builder().nombre("Admin").apellidos("Salón")
                .email("admin-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password("x").role(Role.ADMIN).build());
        laura = datos.clienta("Laura");
        marta = datos.clienta("Marta");
        sindi = datos.trabajadora("Sindi");
        servicio = datos.servicio("Semipermanente", 60);
    }

    // ---------------------------------------------------------------- mis citas (clienta)

    @Nested
    class MisCitas {

        @Test
        void characterises_mis_citas() {
            datos.cita(laura, sindi, servicio, LUNES.atTime(10, 0), EstadoCita.CONFIRMADA);
            datos.cita(laura, sindi, servicio, LUNES.plusDays(1).atTime(11, 0), EstadoCita.CANCELADA);
            datos.cita(marta, sindi, servicio, LUNES.atTime(12, 0), EstadoCita.CONFIRMADA);
            assertThat(resumen(peticion(HttpMethod.GET, "/api/citas/mis-citas", laura))).isEqualTo("200 [{\"id\":N,\"fechaHoraInicio\":\"2030-02-05T11:00:00\",\"fechaHoraFin\":\"2030-02-05T12:00:00\",\"estado\":\"CANCELADA\",\"servicio\":{\"id\":N,\"nombre\":\"Semipermanente\",\"precio\":20.0,\"duracionEnMinutos\":60},\"trabajador\":{\"id\":N,\"nombre\":\"Sindi\",\"apellidos\":\"García\"},\"cliente\":{\"id\":N,\"nombre\":\"Laura\",\"apellidos\":\"Pérez\",\"email\":\"laura-XXXX@test.com\"}},{\"id\":N,\"fechaHoraInicio\":\"2030-02-04T10:00:00\",\"fechaHoraFin\":\"2030-02-04T11:00:00\",\"estado\":\"CONFIRMADA\",\"servicio\":{\"id\":N,\"nombre\":\"Semipermanente\",\"precio\":20.0,\"duracionEnMinutos\":60},\"trabajador\":{\"id\":N,\"nombre\":\"Sindi\",\"apellidos\":\"García\"},\"cliente\":{\"id\":N,\"nombre\":\"Laura\",\"apellidos\":\"Pérez\",\"email\":\"laura-XXXX@test.com\"}}]");
        }

        @Test
        void characterises_mis_citas_sin_citas() {
            assertThat(resumen(peticion(HttpMethod.GET, "/api/citas/mis-citas", laura))).isEqualTo("200 []");
        }

        @Test
        void characterises_mis_citas_siendo_administradora() {
            assertThat(resumen(peticion(HttpMethod.GET, "/api/citas/mis-citas", admin))).isEqualTo("200 []");
        }
    }

    // ---------------------------------------------------------------- todas las citas (administración)

    @Nested
    class TodasLasCitas {

        @Test
        void characterises_listado_de_la_administracion() throws Exception {
            datos.cita(laura, sindi, servicio, LUNES.atTime(10, 0), EstadoCita.CONFIRMADA);
            datos.cita(marta, sindi, servicio, LUNES.plusDays(1).atTime(11, 0), EstadoCita.CANCELADA);
            datos.cita(laura, sindi, servicio, LUNES.minusDays(1).atTime(9, 0), EstadoCita.COMPLETADA);
            var r = peticion(HttpMethod.GET, "/api/admin/citas", admin);
            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(deSindi(r)).isEqualTo("[{\"id\":N,\"fechaHoraInicio\":\"2030-02-05T11:00:00\",\"fechaHoraFin\":\"2030-02-05T12:00:00\",\"estado\":\"CANCELADA\",\"servicio\":{\"id\":N,\"nombre\":\"Semipermanente\",\"precio\":20.0,\"duracionEnMinutos\":60},\"trabajador\":{\"id\":N,\"nombre\":\"Sindi\",\"apellidos\":\"García\"},\"cliente\":{\"id\":N,\"nombre\":\"Marta\",\"apellidos\":\"Pérez\",\"email\":\"marta-XXXX@test.com\"}},{\"id\":N,\"fechaHoraInicio\":\"2030-02-04T10:00:00\",\"fechaHoraFin\":\"2030-02-04T11:00:00\",\"estado\":\"CONFIRMADA\",\"servicio\":{\"id\":N,\"nombre\":\"Semipermanente\",\"precio\":20.0,\"duracionEnMinutos\":60},\"trabajador\":{\"id\":N,\"nombre\":\"Sindi\",\"apellidos\":\"García\"},\"cliente\":{\"id\":N,\"nombre\":\"Laura\",\"apellidos\":\"Pérez\",\"email\":\"laura-XXXX@test.com\"}},{\"id\":N,\"fechaHoraInicio\":\"2030-02-03T09:00:00\",\"fechaHoraFin\":\"2030-02-03T10:00:00\",\"estado\":\"COMPLETADA\",\"servicio\":{\"id\":N,\"nombre\":\"Semipermanente\",\"precio\":20.0,\"duracionEnMinutos\":60},\"trabajador\":{\"id\":N,\"nombre\":\"Sindi\",\"apellidos\":\"García\"},\"cliente\":{\"id\":N,\"nombre\":\"Laura\",\"apellidos\":\"Pérez\",\"email\":\"laura-XXXX@test.com\"}}]");
        }
    }

    // ---------------------------------------------------------------- resumen semanal

    @Nested
    class ResumenSemanal {

        @Test
        void characterises_forma_del_resumen_semanal() {
            var r = peticion(HttpMethod.GET, "/api/admin/stats/citas-semanales", admin);
            String forma = String.valueOf(r.getBody())
                    .replaceAll("\"name\":\"[^\"]+\"", "\"name\":D")
                    .replaceAll("\"Citas\":\\d+", "\"Citas\":N");
            assertThat(r.getStatusCode().value() + " " + forma).isEqualTo("200 [{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N},{\"name\":D,\"Citas\":N}]");
        }

        @Test
        void characterises_nombres_de_los_dias() throws Exception {
            var r = peticion(HttpMethod.GET, "/api/admin/stats/citas-semanales", admin);
            String nombres = StreamSupport.stream(json.readTree(r.getBody()).spliterator(), false)
                    .map(n -> n.path("name").asText())
                    .sorted()
                    .collect(Collectors.joining(","));
            assertThat(nombres).isEqualTo("dom,jue,lun,mar,mié,sáb,vie");
        }
    }

    // ---------------------------------------------------------------- disponibilidad de un día

    @Nested
    class Disponibilidad {

        @Test
        void characterises_huecos_de_un_dia() {
            datos.turno(sindi, LUNES, "10:00", "13:00");
            datos.cita(laura, sindi, servicio, LUNES.atTime(10, 30), EstadoCita.CONFIRMADA);
            assertThat(resumen(disponibilidad("trabajadorId=" + sindi.getId() + "&servicioId=" + servicio.getId()
                    + "&fecha=" + LUNES))).isEqualTo("200 [\"11:30\",\"11:45\",\"12:00\"]");
        }

        @Test
        void characterises_huecos_sin_turno() {
            assertThat(resumen(disponibilidad("trabajadorId=" + sindi.getId() + "&servicioId=" + servicio.getId()
                    + "&fecha=" + LUNES))).isEqualTo("200 []");
        }

        @Test
        void characterises_huecos_de_una_trabajadora_que_no_existe() {
            assertThat(resumen(disponibilidad("trabajadorId=99999999&servicioId=" + servicio.getId()
                    + "&fecha=" + LUNES))).isEqualTo("404 {\"message\":\"Trabajador no encontrado con id: 99999999\"}");
        }

        @Test
        void characterises_huecos_de_un_servicio_que_no_existe() {
            assertThat(resumen(disponibilidad("trabajadorId=" + sindi.getId() + "&servicioId=99999999&fecha=" + LUNES)))
                    .isEqualTo("404 {\"message\":\"Servicio no encontrado con id: 99999999\"}");
        }
    }

    // ---------------------------------------------------------------- días no disponibles de un mes

    @Nested
    class DiasNoDisponibles {

        @Test
        void characterises_dias_no_disponibles_de_un_mes() {
            datos.turno(sindi, LUNES, "10:00", "12:00");
            datos.turno(sindi, LUNES.plusDays(1), "10:00", "12:00");
            datos.cita(laura, sindi, servicio, LUNES.plusDays(1).atTime(10, 0), EstadoCita.CONFIRMADA);
            datos.cita(laura, sindi, servicio, LUNES.plusDays(1).atTime(11, 0), EstadoCita.CONFIRMADA);
            // Devuelve las 42 casillas del calendario (6 semanas desde el lunes de la semana del día 1), no solo el mes.
            assertThat(resumen(diasNoDisponibles("anio=2030&mes=2"))).isEqualTo("200 [\"2030-01-28\",\"2030-01-29\",\"2030-01-30\",\"2030-01-31\",\"2030-02-01\",\"2030-02-02\",\"2030-02-03\",\"2030-02-05\",\"2030-02-06\",\"2030-02-07\",\"2030-02-08\",\"2030-02-09\",\"2030-02-10\",\"2030-02-11\",\"2030-02-12\",\"2030-02-13\",\"2030-02-14\",\"2030-02-15\",\"2030-02-16\",\"2030-02-17\",\"2030-02-18\",\"2030-02-19\",\"2030-02-20\",\"2030-02-21\",\"2030-02-22\",\"2030-02-23\",\"2030-02-24\",\"2030-02-25\",\"2030-02-26\",\"2030-02-27\",\"2030-02-28\",\"2030-03-01\",\"2030-03-02\",\"2030-03-03\",\"2030-03-04\",\"2030-03-05\",\"2030-03-06\",\"2030-03-07\",\"2030-03-08\",\"2030-03-09\",\"2030-03-10\"]");
        }
    }

    // ---------------------------------------------------------------- utilidades

    private ResponseEntity<String> disponibilidad(String parametros) {
        return peticion(HttpMethod.GET, "/api/citas/disponibilidad?" + parametros, null);
    }

    private ResponseEntity<String> diasNoDisponibles(String parametros) {
        return peticion(HttpMethod.GET, "/api/citas/dias-no-disponibles?trabajadorId=" + sindi.getId()
                + "&servicioId=" + servicio.getId() + "&" + parametros, null);
    }

    /** Las citas de la trabajadora de este test dentro del listado compartido, en el orden en que llegan. */
    private String deSindi(ResponseEntity<String> r) throws Exception {
        JsonNode lista = json.readTree(r.getBody());
        return StreamSupport.stream(lista.spliterator(), false)
                .filter(n -> n.path("trabajador").path("id").asLong() == sindi.getId())
                .map(n -> enmascarar(n.toString()))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String url, Usuario usuario) {
        HttpHeaders h = new HttpHeaders();
        if (usuario != null) h.setBearerAuth(jwtService.generateToken(usuario));
        return http.exchange(url, metodo, new HttpEntity<>(null, h), String.class);
    }

    private static String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + enmascarar(String.valueOf(r.getBody()));
    }

    /** Sin ids, emails aleatorios ni marcas de tiempo. */
    private static String enmascarar(String texto) {
        return texto
                .replaceAll("\"id\":\\d+", "\"id\":N")
                .replaceAll("-[0-9a-f]{8}@test\\.com", "-XXXX@test.com")
                .replaceAll("Id=\\d+", "Id=N")
                .replaceAll("\"timestamp\":\"[^\"]+\"", "\"timestamp\":T");
    }
}
