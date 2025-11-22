package com.SindiNails.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SindiNails.backend.model.Cita;

public interface RepositorioCita extends JpaRepository<Cita, Long> {

    List<Cita> findByClienteIdOrderByFechaHoraInicioDesc(Long clienteId);

    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId AND c.estado != 'CANCELADA' AND ((c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio))")
    List<Cita> findCitasSolapadas(@Param("trabajadorId") Long trabajadorId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
