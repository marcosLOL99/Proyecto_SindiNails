package com.SindiNails.backend.service;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.dto.CitaDto;
import com.SindiNails.backend.dto.CitasPorDiaDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CHARACTERISATION TESTS -- documentan el comportamiento ACTUAL de CitaService, NO lo que debería hacer.
 *
 * Fijan lo que el código hace hoy (incluidas las consultas JPQL contra un MySQL real) para poder
 * refactorizarlo con seguridad. Los casos marcados como SUSPICIOUS parecen bugs: se documentan
 * tal cual y se decide con el usuario si se corrigen (con TDD, como cambio de comportamiento aparte).
 *
 * Son temporales: se sustituirán por tests de comportamiento cuando CitaService se refactorice.
 * Ver la skill characterisation-tests.
 *
 * Las fechas son relativas a hoy: esta clase usa el reloj real de la aplicación (ClockConfig) y no RelojFijo,
 * porque se escribió antes de que CitaService recibiera el Clock inyectado.
 * La clase es @Transactional: cada test se revierte y la sesión JPA queda abierta, igual que en una
 * petición HTTP real (open-in-view), necesario porque convertToDto recorre relaciones LAZY.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class CitaServiceCharacterisationTest {

    /** Un día cualquiera dentro de un año: siempre futuro, sin depender de la fecha de ejecución. */
    private static final LocalDate FECHA = LocalDate.now().plusYears(1);

    @Autowired CitaService citaService;
    @Autowired RepositorioCita repositorioCita;
    @Autowired RepositorioTrabajador repositorioTrabajador;
    @Autowired RepositorioServicios repositorioServicios;
    @Autowired RepositorioTipoServicio repositorioTipoServicio;
    @Autowired RepositorioUsuario repositorioUsuario;
    @Autowired RepositorioTurno repositorioTurno;
    @Autowired RopositorioPeriodoNoDisp repositorioPeriodos;

    private Trabajador trabajadora;
    private Trabajador otraTrabajadora;
    private ServicioLocal servicio60;
    private ServicioLocal servicio45;
    private Usuario clienta;
    private Usuario otraClienta;

    @BeforeEach
    void datosBase() {
        TipoServicio manicura = new TipoServicio();
        manicura.setNombre("Manicura");
        repositorioTipoServicio.save(manicura);

        servicio60 = servicio("Semipermanente", 25.5, 60, manicura);
        servicio45 = servicio("Retirada", 10.0, 45, manicura);
        trabajadora = repositorioTrabajador.save(Trabajador.builder().nombre("Sindi").apellidos("García").build());
        otraTrabajadora = repositorioTrabajador.save(Trabajador.builder().nombre("Ana").apellidos("López").build());
        clienta = usuario("Laura");
        otraClienta = usuario("Marta");
    }

    // ---------------------------------------------------------------- getHorariosDisponibles

    @Nested
    class HorariosDisponibles {

        @Test
        void characterises_sin_turno_ese_dia() {
            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .isEmpty();
        }

        @Test
        void characterises_turno_de_10_a_12_con_servicio_de_60_min() {
            turno(trabajadora, FECHA, "10:00", "12:00");

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("10:00", "10:15", "10:30", "10:45", "11:00");
        }

        @Test
        void characterises_turno_de_10_a_12_con_servicio_de_45_min() {
            turno(trabajadora, FECHA, "10:00", "12:00");

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio45.getId(), FECHA))
                    .containsExactly("10:00", "10:15", "10:30", "10:45", "11:00", "11:15");
        }

        @Test
        void characterises_cita_confirmada_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "13:00");
            cita(clienta, trabajadora, servicio60, FECHA.atTime(10, 30), EstadoCita.CONFIRMADA);

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("11:30", "11:45", "12:00");
        }

        @Test
        void characterises_cita_cancelada_no_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "13:00");
            cita(clienta, trabajadora, servicio60, FECHA.atTime(10, 30), EstadoCita.CANCELADA);

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly(
                    "10:00", "10:15", "10:30", "10:45", "11:00", "11:15", "11:30", "11:45", "12:00");
        }

        @Test
        void characterises_cita_de_otra_trabajadora_no_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "12:00");
            cita(clienta, otraTrabajadora, servicio60, FECHA.atTime(10, 0), EstadoCita.CONFIRMADA);

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("10:00", "10:15", "10:30", "10:45", "11:00");
        }

        @Test
        void characterises_ausencia_de_la_trabajadora_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "13:00");
            bloqueo(TipoBloqueo.TRABAJADOR, trabajadora, FECHA.atTime(11, 0), FECHA.atTime(12, 0));

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("10:00", "12:00");
        }

        @Test
        void characterises_bloqueo_global_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "13:00");
            bloqueo(TipoBloqueo.GLOBAL, null, FECHA.atTime(10, 0), FECHA.atTime(11, 0));

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("11:00", "11:15", "11:30", "11:45", "12:00");
        }

        @Test
        void characterises_ausencia_de_otra_trabajadora_no_ocupa_huecos() {
            turno(trabajadora, FECHA, "10:00", "12:00");
            bloqueo(TipoBloqueo.TRABAJADOR, otraTrabajadora, FECHA.atTime(10, 0), FECHA.atTime(12, 0));

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .containsExactly("10:00", "10:15", "10:30", "10:45", "11:00");
        }

        @Test
        void characterises_ausencia_de_varios_dias_que_cubre_el_dia_entero() {
            turno(trabajadora, FECHA, "10:00", "12:00");
            bloqueo(TipoBloqueo.TRABAJADOR, trabajadora, FECHA.minusDays(1).atTime(0, 0), FECHA.plusDays(1).atTime(0, 0));

            assertThat(citaService.getHorariosDisponibles(trabajadora.getId(), servicio60.getId(), FECHA))
                    .isEmpty();
        }

        @Test
        void characterises_trabajadora_inexistente() {
            assertThatThrownBy(() -> citaService.getHorariosDisponibles(999_999L, servicio60.getId(), FECHA))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Trabajador no encontrado con id: 999999");
        }

        @Test
        void characterises_servicio_inexistente() {
            assertThatThrownBy(() -> citaService.getHorariosDisponibles(trabajadora.getId(), 999_999L, FECHA))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Servicio no encontrado con id: 999999");
        }
    }

    // ---------------------------------------------------------------- getDiasNoDisponibles

    @Nested
    class DiasNoDisponibles {

        @Test
        void characterises_mes_futuro_con_turno_solo_los_dias_1_y_2() {
            LocalDate primero = FECHA.withDayOfMonth(1);
            turno(trabajadora, primero, "10:00", "12:00");
            turno(trabajadora, primero.plusDays(1), "10:00", "12:00");

            List<LocalDate> noDisponibles = citaService.getDiasNoDisponibles(
                    trabajadora.getId(), servicio60.getId(), primero.getYear(), primero.getMonthValue());

            assertThat(noDisponibles).hasSize(40);
            assertThat(diasDisponiblesDeLaRejilla(primero, noDisponibles)).containsExactly(primero, primero.plusDays(1));
            assertThat(noDisponibles).isSorted();
        }

        @Test
        void characterises_mes_pasado_con_turnos() {
            LocalDate primero = LocalDate.now().minusMonths(3).withDayOfMonth(1);
            turno(trabajadora, primero.plusDays(9), "10:00", "12:00");

            List<LocalDate> noDisponibles = citaService.getDiasNoDisponibles(
                    trabajadora.getId(), servicio60.getId(), primero.getYear(), primero.getMonthValue());

            assertThat(noDisponibles).hasSize(42);
        }

        @Test
        void characterises_servicio_inexistente_en_mes_futuro() {
            LocalDate primero = FECHA.withDayOfMonth(1);

            assertThatThrownBy(() -> citaService.getDiasNoDisponibles(
                    trabajadora.getId(), 999_999L, primero.getYear(), primero.getMonthValue()))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Servicio no encontrado con id: 999999");
        }

        /** Días de la rejilla de 42 días (desde el lunes de la semana del día 1) que NO están en la lista. */
        private List<LocalDate> diasDisponiblesDeLaRejilla(LocalDate primero, List<LocalDate> noDisponibles) {
            LocalDate inicioRejilla = primero.with(DayOfWeek.MONDAY);
            return IntStream.range(0, 42).mapToObj(inicioRejilla::plusDays)
                    .filter(d -> !noDisponibles.contains(d))
                    .collect(Collectors.toList());
        }
    }

    // ---------------------------------------------------------------- deleteCita

    @Nested
    class BorrarYCancelar {

        @Test
        void characterises_borrar_cita() {
            Cita existente = cita(clienta, trabajadora, servicio60, FECHA.atTime(10, 0), EstadoCita.CONFIRMADA);

            citaService.deleteCita(existente.getId());

            assertThat(repositorioCita.existsById(existente.getId())).isFalse();
        }

        @Test
        void characterises_borrar_cita_inexistente() {
            assertThatThrownBy(() -> citaService.deleteCita(999_999L))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cita no encontrada con id: 999999");
        }
    }

    // ---------------------------------------------------------------- listados

    @Nested
    class Listados {

        @Test
        void characterises_mis_citas() {
            cita(clienta, trabajadora, servicio60, FECHA.atTime(10, 0), EstadoCita.CONFIRMADA);
            cita(clienta, trabajadora, servicio45, FECHA.plusDays(3).atTime(9, 0), EstadoCita.CANCELADA);
            cita(clienta, otraTrabajadora, servicio60, FECHA.minusDays(5).atTime(17, 0), EstadoCita.COMPLETADA);
            cita(otraClienta, trabajadora, servicio60, FECHA.atTime(12, 0), EstadoCita.CONFIRMADA);

            List<String> misCitas = citaService.getMisCitas(clienta).stream().map(this::lineaRelativa).toList();

            assertThat(misCitas).containsExactly(
                    "FECHA+3 09:00 CANCELADA Laura Sindi", "FECHA+0 10:00 CONFIRMADA Laura Sindi", "FECHA-5 17:00 COMPLETADA Laura Ana");
        }

        @Test
        void characterises_todas_las_citas() {
            cita(clienta, trabajadora, servicio60, FECHA.atTime(10, 0), EstadoCita.CONFIRMADA);
            cita(otraClienta, otraTrabajadora, servicio45, FECHA.plusDays(1).atTime(9, 0), EstadoCita.CANCELADA);
            cita(otraClienta, trabajadora, servicio60, FECHA.minusDays(1).atTime(18, 0), EstadoCita.PENDIENTE);

            List<String> todas = citaService.getAllCitas().stream().map(this::lineaRelativa).toList();

            assertThat(todas).containsExactly(
                    "FECHA+1 09:00 CANCELADA Marta Ana", "FECHA+0 10:00 CONFIRMADA Laura Sindi", "FECHA-1 18:00 PENDIENTE Marta Sindi");
        }

        /** Cita en una línea, con la fecha relativa a FECHA para que no dependa del día de ejecución. */
        private String lineaRelativa(CitaDto c) {
            long dias = java.time.temporal.ChronoUnit.DAYS.between(FECHA, c.getFechaHoraInicio().toLocalDate());
            return "FECHA%+d %s %s %s %s".formatted(dias, c.getFechaHoraInicio().toLocalTime(),
                    c.getEstado(), c.getCliente().getNombre(), c.getTrabajador().getNombre());
        }
    }

    // ---------------------------------------------------------------- utilidades

    private ServicioLocal servicio(String nombre, double precio, int minutos, TipoServicio tipo) {
        ServicioLocal s = new ServicioLocal();
        s.setNombre(nombre);
        s.setDescripcion(nombre);
        s.setPrecio(precio);
        s.setDuracionEnMinutos(minutos);
        s.setTipoServicio(tipo);
        return repositorioServicios.save(s);
    }

    private Usuario usuario(String nombre) {
        return repositorioUsuario.save(Usuario.builder()
                .nombre(nombre).apellidos("Pérez")
                .email(nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .password("x").telefono("600000000").role(Role.USER).build());
    }

    private void turno(Trabajador t, LocalDate fecha, String inicio, String fin) {
        repositorioTurno.save(new Turno(null, t, fecha, LocalTime.parse(inicio), LocalTime.parse(fin)));
    }

    private void bloqueo(TipoBloqueo tipo, Trabajador t, LocalDateTime inicio, LocalDateTime fin) {
        repositorioPeriodos.save(PeriodoNoDisponible.builder()
                .tipo(tipo).trabajador(t).motivo("test").fechaHoraInicio(inicio).fechaHoraFin(fin).build());
    }

    private Cita cita(Usuario cliente, Trabajador t, ServicioLocal s, LocalDateTime inicio, EstadoCita estado) {
        return repositorioCita.save(Cita.builder().cliente(cliente).trabajador(t).servicio(s)
                .fechaHoraInicio(inicio).fechaHoraFin(inicio.plusMinutes(s.getDuracionEnMinutos()))
                .estado(estado).build());
    }

}
