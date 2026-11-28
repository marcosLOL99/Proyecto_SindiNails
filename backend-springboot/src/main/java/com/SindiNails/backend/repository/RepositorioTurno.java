package com.SindiNails.backend.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SindiNails.backend.model.Turno;

public interface RepositorioTurno extends JpaRepository<Turno, Long> {
    List<Turno> findByTrabajadorIdAndFechaOrderByHoraInicioAsc(Long trabajadorId, LocalDate fecha);

    List<Turno> findByTrabajadorIdOrderByFechaAscHoraInicioAsc(Long trabajadorId);
}
