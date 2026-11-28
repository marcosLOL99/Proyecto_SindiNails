package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RepositorioCita extends JpaRepository<Cita, Long> {
    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId AND c.estado != 'CANCELADA' AND c.fechaHoraInicio < :fechaHoraFin AND c.fechaHoraFin > :fechaHoraInicio")
    List<Cita> findCitasSolapadas(@Param("trabajadorId") Long trabajadorId, @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio, @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId AND c.id != :citaId AND c.estado != 'CANCELADA' AND c.fechaHoraInicio < :fechaHoraFin AND c.fechaHoraFin > :fechaHoraInicio")
    List<Cita> findCitasSolapadasExcluyendoCita(@Param("trabajadorId") Long trabajadorId, @Param("fechaHoraInicio") LocalDateTime fechaHoraInicio, @Param("fechaHoraFin") LocalDateTime fechaHoraFin, @Param("citaId") Long citaId);

    @Query("SELECT c FROM Cita c WHERE c.estado != 'CANCELADA' AND c.fechaHoraInicio < :fechaHoraFin AND c.fechaHoraFin > :fechaHoraInicio")
    List<Cita> findCitasSolapadasDeTodas(@Param("fechaHoraInicio") LocalDateTime fechaHoraInicio, @Param("fechaHoraFin") LocalDateTime fechaHoraFin);

    List<Cita> findByClienteIdOrderByFechaHoraInicioDesc(Long clienteId);

    boolean existsByClienteId(Long clienteId);

    List<Cita> findAllByOrderByFechaHoraInicioDesc();

    @Query("SELECT new map(function('DATE', c.fechaHoraInicio) as fecha, count(c.id) as cantidad) " +
           "FROM Cita c " +
           "WHERE c.fechaHoraInicio >= :fechaInicio AND c.estado != 'CANCELADA' " +
           "GROUP BY function('DATE', c.fechaHoraInicio)")
    List<java.util.Map<String, Object>> countCitasFromDate(@Param("fechaInicio") LocalDateTime fechaInicio);
}
