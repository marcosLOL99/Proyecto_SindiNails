package com.SindiNails.backend.service;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.dto.CitasPorDiaDto;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.support.DatosDePrueba;
import com.SindiNails.backend.support.RelojFijo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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
}
