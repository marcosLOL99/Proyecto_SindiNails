package com.SindiNails.backend.service;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.dto.AdminCreateCitaDto;
import com.SindiNails.backend.dto.CitaDto;
import com.SindiNails.backend.dto.CitasPorDiaDto;
import com.SindiNails.backend.dto.CreateCitaDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Cita;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.support.DatosDePrueba;
import com.SindiNails.backend.support.RelojFijo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Comportamiento esperado de CitaService (tests permanentes).
 * "Ahora" está congelado en RelojFijo.AHORA: martes 15/01/2030 a las 10:00.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, RelojFijo.class, DatosDePrueba.class})
@Transactional
class CitaServiceTest {

    private static final LocalDate HOY = RelojFijo.AHORA.toLocalDate();

    @Autowired CitaService citaService;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioCita repositorioCita;

    private Trabajador sindi;
    private ServicioLocal manicura60;
    private Usuario laura;

    @BeforeEach
    void datosBase() {
        sindi = datos.trabajadora("Sindi");
        manicura60 = datos.servicio("Semipermanente", 60);
        laura = datos.clienta("Laura");
    }

    @Nested
    class HuecosLibres {

        @Test
        void hoy_no_ofrece_huecos_que_empiezan_antes_de_la_hora_actual() {
            datos.turno(sindi, HOY, "09:00", "12:00");

            assertThat(citaService.getHorariosDisponibles(sindi.getId(), manicura60.getId(), HOY))
                    .containsExactly("10:00", "10:15", "10:30", "10:45", "11:00");
        }

        @Test
        void con_turno_partido_los_huecos_salen_ordenados_por_hora() {
            LocalDate manana = HOY.plusDays(1);
            datos.turno(sindi, manana, "16:00", "18:00");
            datos.turno(sindi, manana, "09:00", "11:00");

            assertThat(citaService.getHorariosDisponibles(sindi.getId(), manicura60.getId(), manana))
                    .containsExactly("09:00", "09:15", "09:30", "09:45", "10:00",
                            "16:00", "16:15", "16:30", "16:45", "17:00");
        }

        @Test
        void una_trabajadora_inactiva_no_tiene_huecos() {
            Trabajador deBaja = datos.trabajadoraInactiva("Ana");
            datos.turno(deBaja, HOY.plusDays(1), "10:00", "12:00");

            assertThat(citaService.getHorariosDisponibles(deBaja.getId(), manicura60.getId(), HOY.plusDays(1))).isEmpty();
        }

        @Test
        void un_dia_pasado_no_tiene_huecos() {
            LocalDate ayer = HOY.minusDays(1);
            datos.turno(sindi, ayer, "09:00", "12:00");

            assertThat(citaService.getHorariosDisponibles(sindi.getId(), manicura60.getId(), ayer)).isEmpty();
        }
    }

    /**
     * Con el bug, el cálculo de huecos no terminaba nunca, así que cada llamada va con un límite de tiempo.
     * assertTimeoutPreemptively la ejecuta en otro hilo, que no ve una transacción de test sin confirmar:
     * por eso esta clase no es transaccional y borra sus datos al terminar.
     */
    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class HuecosCercaDeMedianoche {

        private final LocalDate manana = HOY.plusDays(1);

        @Autowired JdbcTemplate jdbc;

        @AfterEach
        void borrarDatosConfirmados() {
            jdbc.update("DELETE FROM turnos WHERE trabajador_id = ?", sindi.getId());
            jdbc.update("DELETE FROM servicio_local WHERE id = ?", manicura60.getId());
            jdbc.update("DELETE FROM tipo_servicio WHERE id = ?", manicura60.getTipoServicio().getId());
            jdbc.update("DELETE FROM trabajadores WHERE id = ?", sindi.getId());
            jdbc.update("DELETE FROM usuarios WHERE id = ?", laura.getId());
        }

        @Test
        void un_turno_que_acaba_a_las_23_59_solo_ofrece_huecos_que_terminan_ese_mismo_dia() {
            datos.turno(sindi, manana, "22:00", "23:59");

            List<String> huecos = assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> citaService.getHorariosDisponibles(sindi.getId(), manicura60.getId(), manana));

            assertThat(huecos).containsExactly("22:00", "22:15", "22:30", "22:45");
        }

        @Test
        void con_un_servicio_corto_el_ultimo_hueco_del_dia_no_vuelve_a_empezar_a_las_00_00() {
            ServicioLocal retoque10 = datos.servicio("Retoque", 10);
            datos.turno(sindi, manana, "23:30", "23:59");

            try {
                List<String> huecos = assertTimeoutPreemptively(Duration.ofSeconds(5),
                        () -> citaService.getHorariosDisponibles(sindi.getId(), retoque10.getId(), manana));

                assertThat(huecos).containsExactly("23:30", "23:45");
            } finally {
                jdbc.update("DELETE FROM servicio_local WHERE id = ?", retoque10.getId());
                jdbc.update("DELETE FROM tipo_servicio WHERE id = ?", retoque10.getTipoServicio().getId());
            }
        }
    }

    @Nested
    class Reservar {

        private final LocalDate manana = HOY.plusDays(1);

        @BeforeEach
        void turnoDeManana() {
            datos.turno(sindi, manana, "10:00", "14:00");
        }

        @Test
        void crea_una_cita_confirmada_que_dura_lo_que_el_servicio() {
            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 0)), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
            assertThat(creada.getFechaHoraInicio()).isEqualTo(manana.atTime(10, 0));
            assertThat(creada.getFechaHoraFin()).isEqualTo(manana.atTime(11, 0));
            assertThat(creada.getServicio().getNombre()).isEqualTo("Semipermanente");
            assertThat(creada.getTrabajador().getNombre()).isEqualTo("Sindi");
            assertThat(creada.getCliente().getNombre()).isEqualTo("Laura");
            assertThat(repositorioCita.findById(creada.getId())).isPresent();
        }

        @Test
        void se_puede_reservar_una_cita_que_termina_justo_al_acabar_el_turno() {
            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(13, 0)), laura);

            assertThat(creada.getFechaHoraFin()).isEqualTo(manana.atTime(14, 0));
        }

        @Test
        void no_se_puede_reservar_si_la_trabajadora_no_tiene_turno_ese_dia() {
            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.plusDays(1).atTime(10, 0)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora no trabaja en el horario seleccionado.");
        }

        @Test
        void no_se_puede_reservar_una_cita_que_empieza_antes_del_turno() {
            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.atTime(9, 45)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora no trabaja en el horario seleccionado.");
        }

        @Test
        void no_se_puede_reservar_una_cita_que_termina_despues_del_turno() {
            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.atTime(13, 15)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora no trabaja en el horario seleccionado.");
        }

        @Test
        void no_se_puede_reservar_durante_una_ausencia_de_la_trabajadora() {
            datos.ausencia(sindi, manana.atTime(11, 0), manana.atTime(12, 0));

            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 30)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("El horario seleccionado está bloqueado.");
        }

        @Test
        void no_se_puede_reservar_durante_un_bloqueo_global() {
            datos.bloqueoGlobal(manana.atTime(10, 0), manana.atTime(10, 30));

            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 0)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("El horario seleccionado está bloqueado.");
        }

        @Test
        void se_puede_reservar_justo_cuando_termina_una_ausencia() {
            datos.ausencia(sindi, manana.atTime(10, 0), manana.atTime(11, 0));

            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(11, 0)), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void la_ausencia_de_otra_trabajadora_no_impide_reservar() {
            datos.ausencia(datos.trabajadora("Ana"), manana.atTime(10, 0), manana.atTime(14, 0));

            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 0)), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void no_se_puede_reservar_en_el_pasado() {
            datos.turno(sindi, HOY, "08:00", "12:00");

            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, HOY.atTime(9, 0)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede reservar una cita en el pasado.");
        }

        @Test
        void se_puede_reservar_a_partir_de_la_hora_actual() {
            datos.turno(sindi, HOY, "08:00", "12:00");

            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, RelojFijo.AHORA), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void no_se_puede_reservar_con_una_trabajadora_inactiva() {
            Trabajador deBaja = datos.trabajadoraInactiva("Ana");
            datos.turno(deBaja, manana, "10:00", "14:00");

            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, deBaja, manana.atTime(10, 0)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora seleccionada no está activa.");
        }

        @Test
        void no_se_puede_reservar_encima_de_otra_cita() {
            datos.cita(datos.clienta("Marta"), sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 30)), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("El trabajador no está disponible en el horario seleccionado.");
        }

        @Test
        void se_puede_reservar_justo_a_continuacion_de_otra_cita() {
            datos.cita(datos.clienta("Marta"), sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(11, 0)), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void una_cita_cancelada_no_ocupa_el_hueco() {
            datos.cita(datos.clienta("Marta"), sindi, manicura60, manana.atTime(10, 0), EstadoCita.CANCELADA);

            CitaDto creada = citaService.createCita(reserva(manicura60, sindi, manana.atTime(10, 0)), laura);

            assertThat(creada.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void servicio_inexistente() {
            CreateCitaDto dto = reserva(manicura60, sindi, manana.atTime(10, 0));
            dto.setIdServicio(999_999L);

            assertThatThrownBy(() -> citaService.createCita(dto, laura))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Servicio no encontrado con id: 999999");
        }

        @Test
        void trabajadora_inexistente() {
            CreateCitaDto dto = reserva(manicura60, sindi, manana.atTime(10, 0));
            dto.setIdTrabajador(999_999L);

            assertThatThrownBy(() -> citaService.createCita(dto, laura))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Trabajador no encontrado con id: 999999");
        }

        @Test
        void la_administracion_reserva_para_una_clienta_con_las_mismas_reglas() {
            Usuario marta = datos.clienta("Marta");

            CitaDto creada = citaService.createCitaByAdmin(reservaAdmin(marta.getId(), manana.atTime(10, 0)));

            assertThat(creada.getCliente().getNombre()).isEqualTo("Marta");
            assertThatThrownBy(() -> citaService.createCitaByAdmin(reservaAdmin(marta.getId(), manana.atTime(15, 0))))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora no trabaja en el horario seleccionado.");
        }

        @Test
        void la_administracion_con_una_clienta_inexistente() {
            assertThatThrownBy(() -> citaService.createCitaByAdmin(reservaAdmin(999_999L, manana.atTime(10, 0))))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cliente no encontrado con id: 999999");
        }

        private AdminCreateCitaDto reservaAdmin(Long idCliente, LocalDateTime inicio) {
            AdminCreateCitaDto dto = new AdminCreateCitaDto();
            dto.setIdServicio(manicura60.getId());
            dto.setIdTrabajador(sindi.getId());
            dto.setFechaHoraInicio(inicio);
            dto.setIdCliente(idCliente);
            return dto;
        }
    }

    @Nested
    class Mover {

        private final LocalDate manana = HOY.plusDays(1);

        @BeforeEach
        void turnoDeManana() {
            datos.turno(sindi, manana, "10:00", "18:00");
        }

        @Test
        void mueve_la_cita_a_otra_hora_servicio_y_trabajadora() {
            Trabajador ana = datos.trabajadora("Ana");
            datos.turno(ana, manana, "16:00", "18:00");
            ServicioLocal retirada45 = datos.servicio("Retirada", 45);
            Cita cita = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            CitaDto movida = citaService.updateCita(cita.getId(), reserva(retirada45, ana, manana.atTime(16, 0)));

            assertThat(movida.getFechaHoraInicio()).isEqualTo(manana.atTime(16, 0));
            assertThat(movida.getFechaHoraFin()).isEqualTo(manana.atTime(16, 45));
            assertThat(movida.getServicio().getNombre()).isEqualTo("Retirada");
            assertThat(movida.getTrabajador().getNombre()).isEqualTo("Ana");
            assertThat(movida.getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void puede_solaparse_con_su_propio_horario_anterior() {
            Cita cita = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            CitaDto movida = citaService.updateCita(cita.getId(), reserva(manicura60, sindi, manana.atTime(10, 15)));

            assertThat(movida.getFechaHoraInicio()).isEqualTo(manana.atTime(10, 15));
        }

        @Test
        void no_se_puede_mover_encima_de_otra_cita() {
            Cita cita = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);
            datos.cita(datos.clienta("Marta"), sindi, manicura60, manana.atTime(12, 0), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.updateCita(cita.getId(), reserva(manicura60, sindi, manana.atTime(11, 30))))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("El trabajador no está disponible en el nuevo horario seleccionado.");
        }

        @Test
        void no_se_puede_mover_fuera_del_turno() {
            Cita cita = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.updateCita(cita.getId(), reserva(manicura60, sindi, manana.atTime(17, 30))))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La trabajadora no trabaja en el horario seleccionado.");
        }

        @Test
        void no_se_puede_mover_al_pasado() {
            datos.turno(sindi, HOY, "08:00", "12:00");
            Cita cita = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.updateCita(cita.getId(), reserva(manicura60, sindi, HOY.atTime(8, 0))))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede reservar una cita en el pasado.");
        }

        @Test
        void no_se_puede_mover_una_cita_cancelada() {
            Cita cancelada = datos.cita(laura, sindi, manicura60, manana.atTime(10, 0), EstadoCita.CANCELADA);

            assertThatThrownBy(() -> citaService.updateCita(cancelada.getId(), reserva(manicura60, sindi, manana.atTime(12, 0))))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede modificar una cita cancelada.");
        }

        @Test
        void cita_inexistente() {
            assertThatThrownBy(() -> citaService.updateCita(999_999L, reserva(manicura60, sindi, manana.atTime(10, 0))))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cita no encontrada con id: 999999");
        }
    }

    @Nested
    class Cancelar {

        private static final LocalDateTime AHORA = RelojFijo.AHORA;

        @Test
        void la_clienta_cancela_su_cita_con_mas_de_12_horas_de_antelacion() {
            Cita cita = datos.cita(laura, sindi, manicura60, AHORA.plusHours(12).plusMinutes(1), EstadoCita.CONFIRMADA);

            citaService.cancelarCita(cita.getId(), laura);

            assertThat(repositorioCita.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        }

        @Test
        void se_puede_cancelar_justo_12_horas_antes() {
            Cita cita = datos.cita(laura, sindi, manicura60, AHORA.plusHours(12), EstadoCita.CONFIRMADA);

            citaService.cancelarCita(cita.getId(), laura);

            assertThat(repositorioCita.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CANCELADA);
        }

        @Test
        void no_se_puede_cancelar_con_menos_de_12_horas_de_antelacion() {
            Cita cita = datos.cita(laura, sindi, manicura60, AHORA.plusHours(12).minusMinutes(1), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede cancelar la cita con menos de 12 horas de antelación.");
        }

        @Test
        void no_se_puede_cancelar_una_cita_que_ya_ha_pasado() {
            Cita cita = datos.cita(laura, sindi, manicura60, AHORA.minusDays(2), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No se puede cancelar una cita que ya ha pasado.");
        }

        @Test
        void no_se_puede_cancelar_una_cita_ya_cancelada() {
            Cita cita = datos.cita(laura, sindi, manicura60, AHORA.plusDays(3), EstadoCita.CANCELADA);

            assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("La cita ya está cancelada.");
        }

        @Test
        void una_clienta_no_puede_cancelar_la_cita_de_otra() {
            Usuario marta = datos.clienta("Marta");
            Cita cita = datos.cita(marta, sindi, manicura60, AHORA.plusDays(3), EstadoCita.CONFIRMADA);

            assertThatThrownBy(() -> citaService.cancelarCita(cita.getId(), laura))
                    .isExactlyInstanceOf(IllegalStateException.class)
                    .hasMessage("No tienes permiso para cancelar esta cita.");
            assertThat(repositorioCita.findById(cita.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
        }

        @Test
        void cancelar_una_cita_que_no_existe() {
            assertThatThrownBy(() -> citaService.cancelarCita(999_999L, laura))
                    .isExactlyInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cita no encontrada con id: 999999");
        }
    }

    @Nested
    class ResumenSemanal {

        @Test
        void cuenta_las_citas_de_cada_dia_sin_las_canceladas() {
            datos.cita(laura, sindi, manicura60, HOY.atTime(9, 0), EstadoCita.CONFIRMADA);
            datos.cita(laura, sindi, manicura60, HOY.atTime(12, 0), EstadoCita.CANCELADA);
            datos.cita(laura, sindi, manicura60, HOY.minusDays(2).atTime(10, 0), EstadoCita.COMPLETADA);
            datos.cita(laura, sindi, manicura60, HOY.minusDays(2).atTime(12, 0), EstadoCita.CANCELADA);

            assertThat(citaService.getCitasUltimaSemana())
                    .extracting(CitasPorDiaDto::getName, CitasPorDiaDto::getCitas)
                    .containsExactly(
                            tuple("mié", 0L), tuple("jue", 0L), tuple("vie", 0L), tuple("sáb", 0L),
                            tuple("dom", 1L), tuple("lun", 0L), tuple("mar", 1L));
        }

        @Test
        void solo_cuenta_desde_hace_seis_dias_a_las_00_00_hasta_hoy() {
            datos.cita(laura, sindi, manicura60, HOY.minusDays(6).atTime(0, 0), EstadoCita.COMPLETADA);
            datos.cita(laura, sindi, manicura60, HOY.minusDays(7).atTime(23, 0), EstadoCita.COMPLETADA);
            datos.cita(laura, sindi, manicura60, HOY.plusDays(1).atTime(10, 0), EstadoCita.CONFIRMADA);

            assertThat(citaService.getCitasUltimaSemana())
                    .extracting(CitasPorDiaDto::getCitas)
                    .containsExactly(1L, 0L, 0L, 0L, 0L, 0L, 0L);
        }
    }

    private static CreateCitaDto reserva(ServicioLocal servicio, Trabajador trabajadora, LocalDateTime inicio) {
        CreateCitaDto dto = new CreateCitaDto();
        dto.setIdServicio(servicio.getId());
        dto.setIdTrabajador(trabajadora.getId());
        dto.setFechaHoraInicio(inicio);
        return dto;
    }
}
