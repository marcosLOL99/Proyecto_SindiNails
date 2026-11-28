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
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * CHARACTERISATION TESTS -- documentan lo que hacen HOY toString() y hashCode() de las entidades JPA
 * (generados por @Data de Lombok), NO lo que deberían hacer. Temporales: se sustituyen al corregir el paso 3.10.
 * Ver la skill characterisation-tests.
 *
 * @Transactional: las colecciones LAZY de la trabajadora se cargan dentro de la misma sesión, como en una petición.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
@Transactional
class EntidadesLombokCharacterisationTest {

    @Autowired DatosDePrueba datos;
    @Autowired RepositorioTrabajador trabajadores;
    @Autowired EntityManager em;

    private Trabajador trabajadoraConTurnoYAusencia() {
        Trabajador sindi = datos.trabajadora("Sindi");
        datos.turno(sindi, LocalDate.of(2030, 3, 4), "10:00", "14:00");
        datos.ausencia(sindi, LocalDateTime.of(2030, 3, 5, 10, 0), LocalDateTime.of(2030, 3, 5, 12, 0));
        em.flush();
        em.clear();
        Trabajador cargada = trabajadores.findById(sindi.getId()).orElseThrow();
        cargada.getTurnos().size();
        cargada.getPeriodosNoDisponibles().size();
        return cargada;
    }

    @Test
    void characterises_toString_de_una_trabajadora_con_turnos() {
        Trabajador sindi = trabajadoraConTurnoYAusencia();
        // SUSPICIOUS: recursión trabajadora → turnos → trabajadora…
        assertThat(String.valueOf(catchThrowable(sindi::toString))).isEqualTo("java.lang.StackOverflowError");
    }

    @Test
    void characterises_hashCode_de_una_trabajadora_con_turnos() {
        Trabajador sindi = trabajadoraConTurnoYAusencia();
        // hashCode no desborda: la colección de Hibernate (PersistentBag) no recorre sus elementos.
        assertThat(String.valueOf(catchThrowable(sindi::hashCode))).isEqualTo("null");
    }

    @Test
    void characterises_toString_de_uno_de_sus_turnos() {
        Turno turno = trabajadoraConTurnoYAusencia().getTurnos().get(0);
        // SUSPICIOUS: recursión turno → trabajadora → turnos → turno…
        assertThat(String.valueOf(catchThrowable(turno::toString))).isEqualTo("java.lang.StackOverflowError");
    }

    @Test
    void characterises_toString_de_una_clienta_muestra_la_contrasena() {
        Usuario laura = datos.clienta("Laura");
        // SUSPICIOUS: el hash de la contraseña sale en cualquier log de la clienta.
        assertThat(laura.toString().contains("password=" + laura.getPassword())).isEqualTo(true);
    }

    @Test
    void characterises_toString_de_una_cita_muestra_la_contrasena_de_la_clienta() {
        Usuario laura = datos.clienta("Laura");
        Cita cita = datos.cita(laura, datos.trabajadora("Ana"), datos.servicio("Semipermanente", 60),
                LocalDateTime.of(2030, 3, 4, 10, 0), EstadoCita.CONFIRMADA);
        // SUSPICIOUS: y en cualquier log de una de sus citas.
        assertThat(cita.toString().contains("password=" + laura.getPassword())).isEqualTo(true);
    }
}
