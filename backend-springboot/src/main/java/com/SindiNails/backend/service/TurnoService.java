package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CreateTurnoBatchDto;
import com.SindiNails.backend.dto.CreateUpdateTurnoDto;
import com.SindiNails.backend.dto.TurnoDto;
import com.SindiNails.backend.exception.PeticionNoValidaException;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.model.Turno;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.repository.RepositorioTurno;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final RepositorioTurno turnoRepository;
    private final RepositorioTrabajador trabajadorRepository;

    public List<TurnoDto> getTurnosByTrabajador(Long trabajadorId) {
        if (!trabajadorRepository.existsById(trabajadorId)) {
            throw new ResourceNotFoundException("No se encontró el trabajador con ID: " + trabajadorId);
        }
        return turnoRepository.findByTrabajadorIdOrderByFechaAscHoraInicioAsc(trabajadorId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public TurnoDto createTurno(CreateUpdateTurnoDto turnoDto) {
        validarHoras(turnoDto.getHoraInicio(), turnoDto.getHoraFin());
        Trabajador trabajador = trabajadorRepository.findById(turnoDto.getTrabajadorId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + turnoDto.getTrabajadorId()));

        validarSinSolape(trabajador, turnoDto, null);

        Turno turno = new Turno();
        turno.setTrabajador(trabajador);
        turno.setFecha(turnoDto.getFecha());
        turno.setHoraInicio(turnoDto.getHoraInicio());
        turno.setHoraFin(turnoDto.getHoraFin());

        Turno nuevoTurno = turnoRepository.save(turno);
        return mapToDto(nuevoTurno);
    }

    public TurnoDto updateTurno(Long id, CreateUpdateTurnoDto turnoDto) {
        validarHoras(turnoDto.getHoraInicio(), turnoDto.getHoraFin());
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con ID: " + id));

        Trabajador trabajador = trabajadorRepository.findById(turnoDto.getTrabajadorId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + turnoDto.getTrabajadorId()));

        validarSinSolape(trabajador, turnoDto, id);

        turno.setTrabajador(trabajador);
        turno.setFecha(turnoDto.getFecha());
        turno.setHoraInicio(turnoDto.getHoraInicio());
        turno.setHoraFin(turnoDto.getHoraFin());

        Turno turnoActualizado = turnoRepository.save(turno);
        return mapToDto(turnoActualizado);
    }

    @Transactional
    public List<TurnoDto> createTurnoBatch(CreateTurnoBatchDto batchDto) {
        batchDto.getHorarios().values().forEach(TurnoService::validarDia);
        Trabajador trabajador = trabajadorRepository.findById(batchDto.getTrabajadorId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + batchDto.getTrabajadorId()));

        LocalDate fechaFinSemana = batchDto.getFechaInicioSemana().plusDays(6);
        List<Turno> turnosExistentes = trabajador.getTurnos().stream()
                .filter(t -> !t.getFecha().isBefore(batchDto.getFechaInicioSemana()) && !t.getFecha().isAfter(fechaFinSemana))
                .collect(Collectors.toList());
        
        trabajador.getTurnos().removeAll(turnosExistentes);
        turnoRepository.deleteAll(turnosExistentes);

        List<Turno> nuevosTurnos = new ArrayList<>();
        batchDto.getHorarios().forEach((diaIndex, horario) -> {
            if (horario.isActivo() && horario.getHoraInicio() != null && horario.getHoraFin() != null) {
                LocalDate fechaTurno = batchDto.getFechaInicioSemana().plusDays(diaIndex);
                boolean hasValidBreak = horario.getHoraInicioDescanso() != null && horario.getHoraFinDescanso() != null;

                if (hasValidBreak) {
                    nuevosTurnos.add(new Turno(null, trabajador, fechaTurno, horario.getHoraInicio(), horario.getHoraInicioDescanso()));
                    nuevosTurnos.add(new Turno(null, trabajador, fechaTurno, horario.getHoraFinDescanso(), horario.getHoraFin()));
                } else {
                    nuevosTurnos.add(new Turno(null, trabajador, fechaTurno, horario.getHoraInicio(), horario.getHoraFin()));
                }
            }
        });

        List<Turno> turnosGuardados = turnoRepository.saveAll(nuevosTurnos);
        return turnosGuardados.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void deleteTurno(Long id) {
        if (!turnoRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se encontró el turno con ID: " + id);
        }
        turnoRepository.deleteById(id);
    }

    /** Otro turno de la trabajadora ese día (sin contar el que se edita, turnoId) no puede pisarlo; contiguos sí. */
    private void validarSinSolape(Trabajador trabajador, CreateUpdateTurnoDto turnoDto, Long turnoId) {
        boolean solapa = turnoRepository.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(trabajador.getId(), turnoDto.getFecha())
                .stream()
                .filter(otro -> !otro.getId().equals(turnoId))
                .anyMatch(otro -> otro.getHoraInicio().isBefore(turnoDto.getHoraFin())
                        && turnoDto.getHoraInicio().isBefore(otro.getHoraFin()));
        if (solapa) {
            throw new ReglaDeNegocioException("El turno se solapa con otro turno de la trabajadora ese día.");
        }
    }

    private static void validarHoras(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new PeticionNoValidaException("La hora de fin debe ser posterior a la de inicio.");
        }
    }

    /** Un día activo de la semana: su horario, y el descanso (si lo tiene) estrictamente dentro de él. */
    private static void validarDia(CreateTurnoBatchDto.HorarioDia dia) {
        if (!dia.isActivo() || dia.getHoraInicio() == null || dia.getHoraFin() == null) {
            return;
        }
        validarHoras(dia.getHoraInicio(), dia.getHoraFin());
        if (dia.getHoraInicioDescanso() != null && dia.getHoraFinDescanso() != null) {
            boolean dentro = dia.getHoraInicioDescanso().isAfter(dia.getHoraInicio())
                    && dia.getHoraFinDescanso().isAfter(dia.getHoraInicioDescanso())
                    && dia.getHoraFin().isAfter(dia.getHoraFinDescanso());
            if (!dentro) {
                throw new PeticionNoValidaException("El descanso debe empezar y acabar dentro del horario.");
            }
        }
    }

    private TurnoDto mapToDto(Turno turno) {
        TurnoDto dto = new TurnoDto();
        dto.setId(turno.getId());
        dto.setFecha(turno.getFecha());
        dto.setHoraInicio(turno.getHoraInicio());
        dto.setHoraFin(turno.getHoraFin());
        if (turno.getTrabajador() != null) {
            dto.setTrabajadorId(turno.getTrabajador().getId());
            dto.setTrabajadorNombre(turno.getTrabajador().getNombre());
        }
        return dto;
    }
}