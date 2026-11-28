package com.SindiNails.backend.service;


import com.SindiNails.backend.dto.*;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import com.SindiNails.backend.exception.PeticionNoValidaException;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioServicios;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.repository.RopositorioPeriodoNoDisp;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.repository.RepositorioTurno;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final RepositorioCita repositorioCita;
    private final RepositorioTrabajador repositorioTrabajador;
    private final RepositorioServicios repositorioServicios;
    private final RepositorioUsuario repositorioUsuario;
    private final RopositorioPeriodoNoDisp periodoNoDisponibleRepository;
    private final RepositorioTurno turnoRepository;
    private final Clock clock;

    @Transactional
    public CitaDto createCita(CreateCitaDto createCitaDto, Usuario cliente) {
        Trabajador trabajador = bloquearTrabajadora(createCitaDto.getIdTrabajador());
        return crearCita(createCitaDto, cliente, trabajador);
    }

    private CitaDto crearCita(CreateCitaDto createCitaDto, Usuario cliente, Trabajador trabajador) {
        ServicioLocal servicio = repositorioServicios.findById(createCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + createCitaDto.getIdServicio()));

        LocalDateTime fechaHoraInicio = createCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());
        validarHorarioReservable(trabajador, fechaHoraInicio, fechaHoraFin);

        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadas(trabajador.getId(), fechaHoraInicio,
                fechaHoraFin);
        if (!citasSolapadas.isEmpty()) {
            throw new ReglaDeNegocioException("El trabajador no está disponible en el horario seleccionado.");
        }

        Cita nuevaCita = Cita.builder()
                .cliente(cliente)
                .trabajador(trabajador)
                .servicio(servicio)
                .fechaHoraInicio(fechaHoraInicio)
                .fechaHoraFin(fechaHoraFin)
                .estado(EstadoCita.CONFIRMADA)
                .build();

        Cita citaGuardada = repositorioCita.save(nuevaCita);
        return convertToDto(citaGuardada);
    }

    @Transactional
    public CitaDto createCitaByAdmin(AdminCreateCitaDto createCitaDto) {
        Trabajador trabajador = bloquearTrabajadora(createCitaDto.getIdTrabajador());
        Usuario cliente = repositorioUsuario.findById(createCitaDto.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + createCitaDto.getIdCliente()));

        return crearCita(createCitaDto, cliente, trabajador);
    }

    @Transactional
    public CitaDto updateCita(Long citaId, CreateCitaDto updateCitaDto) {
        Trabajador trabajador = bloquearTrabajadora(updateCitaDto.getIdTrabajador());
        Cita citaExistente = repositorioCita.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));
        if (citaExistente.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaDeNegocioException("No se puede modificar una cita cancelada.");
        }

        ServicioLocal servicio = repositorioServicios.findById(updateCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + updateCitaDto.getIdServicio()));

        LocalDateTime fechaHoraInicio = updateCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());
        // Corregir una cita que ya ha empezado (servicio, trabajadora) sin cambiar su hora se permite aunque la
        // trabajadora ya no esté activa o no tenga turno. Una cita futura cumple siempre las reglas de horario,
        // también si mantiene la hora: si no, podría quedar fuera de turno o dentro de una ausencia.
        // El solape con otras citas se comprueba siempre.
        boolean correccionDeCitaPasada = fechaHoraInicio.equals(citaExistente.getFechaHoraInicio())
                && citaExistente.getFechaHoraInicio().isBefore(LocalDateTime.now(clock));
        if (!correccionDeCitaPasada) {
            validarHorarioReservable(trabajador, fechaHoraInicio, fechaHoraFin);
        }

        // Validar disponibilidad excluyendo la propia cita que se está actualizando
        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadasExcluyendoCita(trabajador.getId(),
                fechaHoraInicio, fechaHoraFin, citaId);
        if (!citasSolapadas.isEmpty()) {
            throw new ReglaDeNegocioException("El trabajador no está disponible en el nuevo horario seleccionado.");
        }

        citaExistente.setTrabajador(trabajador);
        citaExistente.setServicio(servicio);
        citaExistente.setFechaHoraInicio(fechaHoraInicio);
        citaExistente.setFechaHoraFin(fechaHoraFin);

        Cita citaActualizada = repositorioCita.save(citaExistente);
        return convertToDto(citaActualizada);
    }

    /**
     * Primera consulta de toda transacción que reserva o mueve una cita: bloquea la fila de la trabajadora hasta el
     * final, así dos reservas suyas no comprueban el solape a la vez. Tiene que ir antes de cualquier otra lectura:
     * en MySQL (REPEATABLE READ) la primera lectura normal fija la foto de los datos, y si fuera antes del bloqueo
     * la transacción que espera no vería la cita que la otra acaba de guardar.
     */
    private Trabajador bloquearTrabajadora(Long trabajadorId) {
        return repositorioTrabajador.findByIdBloqueando(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));
    }

    /**
     * Reglas comunes para reservar o mover una cita: no en el pasado, con una trabajadora activa,
     * dentro de uno de sus turnos de ese día y sin pisar ausencias ni bloqueos globales.
     * Los solapes con otras citas se comprueban aparte (create y update usan consultas distintas).
     */
    private void validarHorarioReservable(Trabajador trabajador, LocalDateTime inicio, LocalDateTime fin) {
        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new ReglaDeNegocioException("No se puede reservar una cita en el pasado.");
        }
        if (!trabajador.isActivo()) {
            throw new ReglaDeNegocioException("La trabajadora seleccionada no está activa.");
        }

        boolean dentroDeUnTurno = inicio.toLocalDate().equals(fin.toLocalDate())
                && turnoRepository.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(trabajador.getId(), inicio.toLocalDate())
                        .stream()
                        .anyMatch(turno -> !inicio.toLocalTime().isBefore(turno.getHoraInicio())
                                && !fin.toLocalTime().isAfter(turno.getHoraFin()));
        if (!dentroDeUnTurno) {
            throw new ReglaDeNegocioException("La trabajadora no trabaja en el horario seleccionado.");
        }

        boolean bloqueado = !periodoNoDisponibleRepository
                .findByTipoAndFechaHoraInicioBeforeAndFechaHoraFinAfter(TipoBloqueo.GLOBAL, fin, inicio).isEmpty()
                || !periodoNoDisponibleRepository
                .findByTrabajadorIdAndFechaHoraInicioBeforeAndFechaHoraFinAfter(trabajador.getId(), fin, inicio).isEmpty();
        if (bloqueado) {
            throw new ReglaDeNegocioException("El horario seleccionado está bloqueado.");
        }
    }

    public void deleteCita(Long citaId) {
        if (!repositorioCita.existsById(citaId)) {
            throw new ResourceNotFoundException("Cita no encontrada con id: " + citaId);
        }
        repositorioCita.deleteById(citaId);
    }

    @Transactional
    public void cancelarCita(Long citaId, Usuario cliente) {
        Cita cita = repositorioCita.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));

        // 1. Solo la dueña: a cualquier otra se le responde como si la cita no existiera (no revela qué ids existen)
        if (!cita.getCliente().getId().equals(cliente.getId())) {
            throw new ResourceNotFoundException("Cita no encontrada con id: " + citaId);
        }

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaDeNegocioException("La cita ya está cancelada.");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (cita.getFechaHoraInicio().isBefore(ahora)) {
            throw new ReglaDeNegocioException("No se puede cancelar una cita que ya ha pasado.");
        }

        // 2. Verificar la regla de las 12 horas
        LocalDateTime limiteCancelacion = cita.getFechaHoraInicio().minusHours(12);
        if (ahora.isAfter(limiteCancelacion)) {
            throw new ReglaDeNegocioException("No se puede cancelar la cita con menos de 12 horas de antelación.");
        }

        // 3. Cambiar el estado y guardar
        cita.setEstado(EstadoCita.CANCELADA);
        repositorioCita.save(cita);
    }

    public List<CitaDto> getMisCitas(Usuario cliente) {
        List<Cita> citas = repositorioCita.findByClienteIdOrderByFechaHoraInicioDesc(cliente.getId());
        return citas.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<CitaDto> getAllCitas() {
        List<Cita> citas = repositorioCita.findAllByOrderByFechaHoraInicioDesc();
        return citas.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<String> getHorariosDisponibles(Long trabajadorId, Long servicioId, LocalDate fecha) {
        Trabajador trabajador = repositorioTrabajador.findById(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));
        ServicioLocal servicio = repositorioServicios.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));
        if (!trabajador.isActivo()) {
            return new ArrayList<>();
        }

        final int DURACION_SERVICIO = servicio.getDuracionEnMinutos();
        final int INTERVALO_MINUTOS = 15;

        List<LocalTime[]> workingIntervals = getWorkingIntervalsForDay(trabajadorId, fecha);
        if (workingIntervals.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime inicioDelDia = fecha.atStartOfDay();
        LocalDateTime finDelDia = fecha.atTime(LocalTime.MAX);
        List<Cita> citasDelDia = repositorioCita.findCitasSolapadas(trabajadorId, inicioDelDia, finDelDia);
        List<PeriodoNoDisponible> bloqueosDelDia = new ArrayList<>();
        bloqueosDelDia.addAll(periodoNoDisponibleRepository.findByTipoAndFechaHoraInicioBeforeAndFechaHoraFinAfter(TipoBloqueo.GLOBAL, finDelDia, inicioDelDia));
        bloqueosDelDia.addAll(periodoNoDisponibleRepository.findByTrabajadorIdAndFechaHoraInicioBeforeAndFechaHoraFinAfter(trabajadorId, finDelDia, inicioDelDia));

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<String> horariosDisponibles = new ArrayList<>();
        for (LocalTime[] interval : workingIntervals) {
            // Se recorre con fecha y hora completas: con LocalTime, pasar de las 23:59 daría la vuelta a las 00:00.
            LocalDateTime posibleInicio = fecha.atTime(interval[0]);
            LocalDateTime finIntervalo = fecha.atTime(interval[1]);

            while (!posibleInicio.plusMinutes(DURACION_SERVICIO).isAfter(finIntervalo)) {
                LocalDateTime posibleFin = posibleInicio.plusMinutes(DURACION_SERVICIO);

                boolean estaSolapado = isOverlapping(posibleInicio, posibleFin, citasDelDia, bloqueosDelDia);
                boolean yaHaPasado = posibleInicio.isBefore(ahora);

                if (!estaSolapado && !yaHaPasado) {
                    horariosDisponibles.add(posibleInicio.format(DateTimeFormatter.ofPattern("HH:mm")));
                }

                posibleInicio = posibleInicio.plusMinutes(INTERVALO_MINUTOS);
            }
        }
        return horariosDisponibles;
    }

    private List<LocalTime[]> getWorkingIntervalsForDay(Long trabajadorId, LocalDate fecha) {
        // La lógica correcta es simplemente devolver los turnos asignados al trabajador para esa fecha.
        // El cálculo de intersección con el horario del negocio era incorrecto y la causa del error.
        List<Turno> turnos = turnoRepository.findByTrabajadorIdAndFechaOrderByHoraInicioAsc(trabajadorId, fecha);
        return turnos.stream()
                .map(t -> new LocalTime[]{t.getHoraInicio(), t.getHoraFin()})
                .collect(Collectors.toList());
    }

    private boolean isOverlapping(LocalDateTime start, LocalDateTime end, List<Cita> citas, List<PeriodoNoDisponible> bloqueos) {
        for (Cita cita : citas) {
            if (start.isBefore(cita.getFechaHoraFin()) && end.isAfter(cita.getFechaHoraInicio())) {
                return true;
            }
        }
        for (PeriodoNoDisponible bloqueo : bloqueos) {
            if (start.isBefore(bloqueo.getFechaHoraFin()) && end.isAfter(bloqueo.getFechaHoraInicio())) {
                return true;
            }
        }
        return false;
    }

    public List<LocalDate> getDiasNoDisponibles(Long trabajadorId, Long servicioId, int anio, int mes) {
        if (mes < 1 || mes > 12) {
            throw new PeticionNoValidaException("El mes debe estar entre 1 y 12.");
        }
        LocalDate primerDiaDelMes = LocalDate.of(anio, mes, 1);
        LocalDate fechaInicioRango = primerDiaDelMes.with(DayOfWeek.MONDAY);

        List<LocalDate> diasNoDisponibles = new ArrayList<>();

        for (int i = 0; i < 42; i++) {
            LocalDate fechaActual = fechaInicioRango.plusDays(i);
            if (fechaActual.isBefore(LocalDate.now(clock))) {
                diasNoDisponibles.add(fechaActual);
            } else if (getHorariosDisponibles(trabajadorId, servicioId, fechaActual).isEmpty()) {
                diasNoDisponibles.add(fechaActual);
            }
        }
        return diasNoDisponibles;
    }

    public List<CitasPorDiaDto> getCitasUltimaSemana() {
        LocalDate hoy = LocalDate.now(clock);
        LocalDateTime fechaInicio = hoy.minusDays(6).atStartOfDay();

        List<Map<String, Object>> resultadosDb = repositorioCita.countCitasFromDate(fechaInicio);

        Map<LocalDate, Long> conteoPorFecha = resultadosDb.stream()
                .collect(Collectors.toMap(
                        r -> ((java.sql.Date) r.get("fecha")).toLocalDate(),
                        r -> (Long) r.get("cantidad")));

        return IntStream.range(0, 7)
                .mapToObj(i -> hoy.minusDays(i))
                .map(fecha -> {
                    String diaSemana = fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
                    long cantidad = conteoPorFecha.getOrDefault(fecha, 0L);
                    return new CitasPorDiaDto(diaSemana, cantidad);
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            java.util.Collections.reverse(list);
                            return list;
                        }));
    }

    private CitaDto convertToDto(Cita cita) {
        ServicioResumenDto servicioDto = ServicioResumenDto.builder()
                .id(cita.getServicio().getId())
                .nombre(cita.getServicio().getNombre())
                .precio(cita.getServicio().getPrecio())
                .duracionEnMinutos(cita.getServicio().getDuracionEnMinutos())
                .build();

        TrabajadorDto trabajadorDto = TrabajadorDto.builder()
                .id(cita.getTrabajador().getId())
                .nombre(cita.getTrabajador().getNombre())
                .apellidos(cita.getTrabajador().getApellidos())
                .build();

        UsuarioResumenDto clienteDto = UsuarioResumenDto.builder()
                .id(cita.getCliente().getId())
                .nombre(cita.getCliente().getNombre())
                .apellidos(cita.getCliente().getApellidos())
                .email(cita.getCliente().getEmail())
                .build();

        return CitaDto.builder()
                .id(cita.getId())
                .fechaHoraInicio(cita.getFechaHoraInicio())
                .fechaHoraFin(cita.getFechaHoraFin())
                .estado(cita.getEstado())
                .servicio(servicioDto)
                .trabajador(trabajadorDto)
                .cliente(clienteDto)
                .build();
    }
}