package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.BloqueoDto;
import com.SindiNails.backend.exception.PeticionNoValidaException;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Cita;
import com.SindiNails.backend.model.PeriodoNoDisponible;
import com.SindiNails.backend.model.TipoBloqueo;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RopositorioPeriodoNoDisp;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodoNoDisponibleService {

    private final RopositorioPeriodoNoDisp periodoNoDisponibleRepository;
    private final RepositorioTrabajador trabajadorRepository;
    private final RepositorioCita citaRepository;
    private final Clock clock;

    public BloqueoDto crearBloqueo(BloqueoDto bloqueoDto) {
        if (!bloqueoDto.getFechaHoraFin().isAfter(bloqueoDto.getFechaHoraInicio())) {
            throw new PeticionNoValidaException("La fecha y hora de fin deben ser posteriores a las de inicio.");
        }
        PeriodoNoDisponible periodo = new PeriodoNoDisponible();
        periodo.setFechaHoraInicio(bloqueoDto.getFechaHoraInicio());
        periodo.setFechaHoraFin(bloqueoDto.getFechaHoraFin());
        periodo.setMotivo(bloqueoDto.getMotivo());
        periodo.setTipo(bloqueoDto.getTipo());

        if (bloqueoDto.getTipo() == TipoBloqueo.TRABAJADOR) {
            if (bloqueoDto.getTrabajadorId() == null) {
                throw new PeticionNoValidaException("El ID del trabajador es obligatorio para bloqueos de tipo TRABAJADOR.");
            }
            Trabajador trabajador = trabajadorRepository.findById(bloqueoDto.getTrabajadorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + bloqueoDto.getTrabajadorId()));
            periodo.setTrabajador(trabajador);
        }

        validarSinCitasDentro(periodo);

        PeriodoNoDisponible savedPeriodo = periodoNoDisponibleRepository.save(periodo);
        return toDto(savedPeriodo);
    }

    public List<BloqueoDto> getBloqueosPorTrabajador(Long trabajadorId) {
        if (!trabajadorRepository.existsById(trabajadorId)) {
            throw new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId);
        }
        return periodoNoDisponibleRepository.findByTrabajadorIdOrderByFechaHoraInicioAsc(trabajadorId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BloqueoDto> getBloqueosGlobales() {
        return periodoNoDisponibleRepository.findByTipoOrderByFechaHoraInicioAsc(TipoBloqueo.GLOBAL).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public void eliminarBloqueo(Long id) {
        if (!periodoNoDisponibleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Periodo de bloqueo no encontrado con id: " + id);
        }
        periodoNoDisponibleRepository.deleteById(id);
    }

    /**
     * Una ausencia no puede pisar citas sin cancelar y todavía por empezar de su trabajadora,
     * ni un bloqueo global las de ninguna. Las citas pasadas no cuentan.
     */
    private void validarSinCitasDentro(PeriodoNoDisponible periodo) {
        List<Cita> pisadas = periodo.getTipo() == TipoBloqueo.TRABAJADOR
                ? citaRepository.findCitasSolapadas(periodo.getTrabajador().getId(),
                        periodo.getFechaHoraInicio(), periodo.getFechaHoraFin())
                : citaRepository.findCitasSolapadasDeTodas(periodo.getFechaHoraInicio(), periodo.getFechaHoraFin());
        LocalDateTime ahora = LocalDateTime.now(clock);
        if (pisadas.stream().anyMatch(cita -> cita.getFechaHoraInicio().isAfter(ahora))) {
            throw new ReglaDeNegocioException("Hay citas sin cancelar en ese periodo. Muévelas o cancélalas antes.");
        }
    }

    private BloqueoDto toDto(PeriodoNoDisponible periodo) {
        BloqueoDto dto = new BloqueoDto();
        dto.setId(periodo.getId());
        dto.setFechaHoraInicio(periodo.getFechaHoraInicio());
        dto.setFechaHoraFin(periodo.getFechaHoraFin());
        dto.setMotivo(periodo.getMotivo());
        dto.setTipo(periodo.getTipo());
        if (periodo.getTrabajador() != null) {
            dto.setTrabajadorId(periodo.getTrabajador().getId());
        }
        return dto;
    }
}

