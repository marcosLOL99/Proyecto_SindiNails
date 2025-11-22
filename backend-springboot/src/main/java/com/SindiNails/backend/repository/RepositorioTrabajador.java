package com.SindiNails.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.SindiNails.backend.model.Trabajador;

public interface RepositorioTrabajador extends JpaRepository<Trabajador, Long> {
    List<Trabajador> findAllByActivoTrue();
}
