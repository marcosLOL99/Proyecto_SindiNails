package com.SindiNails.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SindiNails.backend.model.DiaSemana;
import com.SindiNails.backend.model.Turno;

public interface RepositorioTurno extends JpaRepository<Turno, Long> {
    List<Turno> findByTrabajadorIdAndDiaDeLaSemana(Long trabajadorId, DiaSemana diaDeLaSemana);
}
