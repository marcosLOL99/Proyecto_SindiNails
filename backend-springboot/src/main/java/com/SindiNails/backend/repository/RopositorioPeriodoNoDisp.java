package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.PeriodoNoDisponible;
import com.SindiNails.backend.model.TipoBloqueo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RopositorioPeriodoNoDisp extends JpaRepository<PeriodoNoDisponible, Long> {

    List<PeriodoNoDisponible> findByTrabajadorId(Long trabajadorId);

    List<PeriodoNoDisponible> findByTipo(TipoBloqueo tipo);

    List<PeriodoNoDisponible> findByTipoAndFechaHoraInicioBeforeAndFechaHoraFinAfter(TipoBloqueo tipo, LocalDateTime fin, LocalDateTime inicio);

    List<PeriodoNoDisponible> findByTrabajadorIdAndFechaHoraInicioBeforeAndFechaHoraFinAfter(Long trabajadorId, LocalDateTime fin, LocalDateTime inicio);
}

