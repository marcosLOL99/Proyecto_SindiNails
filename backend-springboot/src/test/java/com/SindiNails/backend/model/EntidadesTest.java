package com.SindiNails.backend.model;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.support.DatosDePrueba;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Las entidades JPA se pueden escribir en un log sin romper nada ni filtrar datos: toString() no entra en
 * recursión por las relaciones en los dos sentidos (trabajadora ↔ turnos, ausencias) ni muestra la contraseña.
 *
 * @Transactional: las colecciones LAZY de la trabajadora se cargan dentro de la misma sesión, como en una petición.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
@Transactional
class EntidadesTest {

    @Autowired DatosDePrueba datos;
    @Autowired RepositorioTrabajador trabajadores;
    @Autowired EntityManager em;

    @Test
    void una_trabajadora_con_turnos_y_ausencias_y_cada_uno_de_ellos_se_pueden_escribir_en_un_log() {
        Trabajador sindi = datos.trabajadora("Sindi");
        datos.turno(sindi, LocalDate.of(2030, 3, 4), "10:00", "14:00");
        datos.ausencia(sindi, LocalDateTime.of(2030, 3, 5, 10, 0), LocalDateTime.of(2030, 3, 5, 12, 0));
        em.flush();
        em.clear();
        Trabajador cargada = trabajadores.findById(sindi.getId()).orElseThrow();
        Turno turno = cargada.getTurnos().get(0);
        PeriodoNoDisponible ausencia = cargada.getPeriodosNoDisponibles().get(0);

        assertThatCode(cargada::toString).doesNotThrowAnyException();
        assertThatCode(cargada::hashCode).doesNotThrowAnyException();
        assertThatCode(turno::toString).doesNotThrowAnyException();
        assertThatCode(ausencia::toString).doesNotThrowAnyException();
    }

    @Test
    void ni_una_clienta_ni_sus_citas_muestran_la_contrasena_en_un_log() {
        Usuario laura = datos.clienta("Laura");
        laura.setPassword("$2a$10$hashDeLaContrasenaDeLaura");
        Cita cita = datos.cita(laura, datos.trabajadora("Ana"), datos.servicio("Semipermanente", 60),
                LocalDateTime.of(2030, 3, 4, 10, 0), EstadoCita.CONFIRMADA);

        assertThat(laura.toString()).doesNotContain(laura.getPassword());
        assertThat(cita.toString()).doesNotContain(laura.getPassword());
    }
}
