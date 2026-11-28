package com.SindiNails.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import com.SindiNails.backend.model.Trabajador;

public interface RepositorioTrabajador extends JpaRepository<Trabajador, Long> {

    /**
     * Carga la trabajadora bloqueando su fila (SELECT ... FOR UPDATE) hasta que acabe la transacción:
     * así las reservas y cambios de cita de una misma trabajadora se hacen de una en una.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trabajador t WHERE t.id = :id")
    Optional<Trabajador> findByIdBloqueando(@Param("id") Long id);
    List<Trabajador> findAllByActivoTrue();
}
