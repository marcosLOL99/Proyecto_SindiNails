package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CreateTurnoBatchDto;
import com.SindiNails.backend.dto.CreateUpdateTurnoDto;
import com.SindiNails.backend.dto.TurnoDto;
import com.SindiNails.backend.exception.PeticionNoValidaException;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Cita;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.model.Turno;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.repository.RepositorioTurno;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final RepositorioTurno turnoRepository;
    private final RepositorioTrabajador trabajadorRepository;
    private final RepositorioCita citaRepository;
    private final Clock clock;

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

    /** Ver {@link #bloquearTrabajadoras}: por qué READ COMMITTED. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TurnoDto updateTurno(Long id, CreateUpdateTurnoDto turnoDto) {
        validarHoras(turnoDto.getHoraInicio(), turnoDto.getHoraFin());
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con ID: " + id));

        bloquearTrabajadoras(turno.getTrabajador().getId(), turnoDto.getTrabajadorId());
        Trabajador trabajador = trabajadorRepository.findById(turnoDto.getTrabajadorId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + turnoDto.getTrabajadorId()));

        validarSinSolape(trabajador, turnoDto, id);

        Long trabajadorAnterior = turno.getTrabajador().getId();
        LocalDate fechaAnterior = turno.getFecha();
        List<Franja> quedan = franjasDelDiaSin(trabajadorAnterior, fechaAnterior, id);
        if (trabajadorAnterior.equals(trabajador.getId()) && fechaAnterior.equals(turnoDto.getFecha())) {
            quedan.add(new Franja(turnoDto.getHoraInicio(), turnoDto.getHoraFin()));
        }
        validarCitasCubiertas(trabajadorAnterior, fechaAnterior, quedan);

        turno.setTrabajador(trabajador);
        turno.setFecha(turnoDto.getFecha());
        turno.setHoraInicio(turnoDto.getHoraInicio());
        turno.setHoraFin(turnoDto.getHoraFin());

        Turno turnoActualizado = turnoRepository.save(turno);
        return mapToDto(turnoActualizado);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TurnoDto> createTurnoBatch(CreateTurnoBatchDto batchDto) {
        batchDto.getHorarios().forEach(TurnoService::validarDia);
        Trabajador trabajador = trabajadorRepository.findByIdBloqueando(batchDto.getTrabajadorId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + batchDto.getTrabajadorId()));

        List<Turno> nuevosTurnos = new ArrayList<>();
        batchDto.getHorarios().forEach((diaIndex, horario) -> {
            if (horario.isActivo()) {
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

        for (int dia = 0; dia <= 6; dia++) {
            LocalDate fecha = batchDto.getFechaInicioSemana().plusDays(dia);
            List<Franja> franjas = nuevosTurnos.stream()
                    .filter(t -> t.getFecha().equals(fecha))
                    .map(t -> new Franja(t.getHoraInicio(), t.getHoraFin()))
                    .collect(Collectors.toList());
            validarCitasCubiertas(trabajador.getId(), fecha, franjas);
        }

        LocalDate fechaFinSemana = batchDto.getFechaInicioSemana().plusDays(6);
        List<Turno> turnosExistentes = trabajador.getTurnos().stream()
                .filter(t -> !t.getFecha().isBefore(batchDto.getFechaInicioSemana()) && !t.getFecha().isAfter(fechaFinSemana))
                .collect(Collectors.toList());
        trabajador.getTurnos().removeAll(turnosExistentes);
        turnoRepository.deleteAll(turnosExistentes);

        List<Turno> turnosGuardados = turnoRepository.saveAll(nuevosTurnos);
        return turnosGuardados.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deleteTurno(Long id) {
        Turno turno = turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el turno con ID: " + id));
        Long trabajadorId = turno.getTrabajador().getId();
        bloquearTrabajadoras(trabajadorId);
        validarCitasCubiertas(trabajadorId, turno.getFecha(), franjasDelDiaSin(trabajadorId, turno.getFecha(), id));
        turnoRepository.delete(turno);
    }

    /**
     * Bloquea las filas de las trabajadoras (SELECT ... FOR UPDATE), en orden de id para que dos transacciones
     * no se esperen en cruz, hasta el final de la transacción. Las reservas toman el mismo bloqueo
     * (CitaService), así que una reserva y un cambio de turnos de la misma trabajadora van de uno en uno.
     * Las transacciones que lo usan son READ COMMITTED: leen el turno antes de saber qué trabajadora bloquear,
     * y con REPEATABLE READ esa primera lectura fijaría la foto de los datos y no verían la cita que una
     * reserva acaba de guardar mientras esperaban.
     */
    private void bloquearTrabajadoras(Long... trabajadorIds) {
        java.util.Arrays.stream(trabajadorIds).distinct().sorted().forEach(id ->
                trabajadorRepository.findByIdBloqueando(id)
                        .orElseThrow(() -> new ResourceNotFoundException("No se encontró el trabajador con ID: " + id)));
    }

    /** Un tramo horario de un día. */
    private record Franja(LocalTime inicio, LocalTime fin) {
        boolean cubre(Cita cita, LocalDate fecha) {
            return cita.getFechaHoraFin().toLocalDate().equals(fecha)
                    && !cita.getFechaHoraInicio().toLocalTime().isBefore(inicio)
                    && !cita.getFechaHoraFin().toLocalTime().isAfter(fin);
        }
    }

    /** Los turnos de la trabajadora ese día, sin el turno turnoId (el que se edita o se borra). */
    private List<Franja> franjasDelDiaSin(Long trabajadorId, LocalDate fecha, Long turnoId) {
        return turnoRepository.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(trabajadorId, fecha).stream()
                .filter(t -> !t.getId().equals(turnoId))
                .map(t -> new Franja(t.getHoraInicio(), t.getHoraFin()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Tras el cambio, cada cita sin cancelar y todavía por empezar de la trabajadora ese día tiene que seguir
     * entera dentro de uno de sus turnos (la misma regla que al reservar). Las citas pasadas no cuentan.
     */
    private void validarCitasCubiertas(Long trabajadorId, LocalDate fecha, List<Franja> franjas) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        boolean algunaFuera = citaRepository.findCitasSolapadas(trabajadorId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                .stream()
                .filter(cita -> cita.getFechaHoraInicio().isAfter(ahora))
                .anyMatch(cita -> franjas.stream().noneMatch(franja -> franja.cubre(cita, fecha)));
        if (algunaFuera) {
            throw new ReglaDeNegocioException(
                    "Hay citas sin cancelar que quedarían fuera del horario de la trabajadora. Muévelas o cancélalas antes.");
        }
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

    /** Un día de la semana (0 = lunes … 6 = domingo); si está activo, su horario y el descanso (si lo tiene) dentro de él. */
    private static void validarDia(Integer diaDeLaSemana, CreateTurnoBatchDto.HorarioDia dia) {
        if (diaDeLaSemana < 0 || diaDeLaSemana > 6) {
            throw new PeticionNoValidaException("Los días de la semana van del 0 (lunes) al 6 (domingo).");
        }
        if (!dia.isActivo()) {
            return;
        }
        if (dia.getHoraInicio() == null || dia.getHoraFin() == null) {
            throw new PeticionNoValidaException("Un día activo necesita hora de inicio y de fin.");
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