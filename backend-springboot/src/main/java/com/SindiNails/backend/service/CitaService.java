package com.SindiNails.backend.service;


import com.SindiNails.backend.dto.*;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioServicios;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.repository.RopositorioPeriodoNoDisp;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.repository.RepositorioTurno;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final Environment env;

    @Transactional
    public CitaDto createCita(CreateCitaDto createCitaDto, Usuario cliente) {
        ServicioLocal servicio = repositorioServicios.findById(createCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + createCitaDto.getIdServicio()));

        Trabajador trabajador = repositorioTrabajador.findById(createCitaDto.getIdTrabajador())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabajador no encontrado con id: " + createCitaDto.getIdTrabajador()));

        LocalDateTime fechaHoraInicio = createCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());

        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadas(trabajador.getId(), fechaHoraInicio,
                fechaHoraFin);
        if (!citasSolapadas.isEmpty()) {
            throw new IllegalStateException("El trabajador no está disponible en el horario seleccionado.");
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
        Usuario cliente = repositorioUsuario.findById(createCitaDto.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente no encontrado con id: " + createCitaDto.getIdCliente()));

        return createCita(createCitaDto, cliente);
    }

    @Transactional
    public CitaDto updateCita(Long citaId, CreateCitaDto updateCitaDto) {
        Cita citaExistente = repositorioCita.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));

        ServicioLocal servicio = repositorioServicios.findById(updateCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Servicio no encontrado con id: " + updateCitaDto.getIdServicio()));

        Trabajador trabajador = repositorioTrabajador.findById(updateCitaDto.getIdTrabajador())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Trabajador no encontrado con id: " + updateCitaDto.getIdTrabajador()));

        LocalDateTime fechaHoraInicio = updateCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());

        // Validar disponibilidad excluyendo la propia cita que se está actualizando
        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadasExcluyendoCita(trabajador.getId(),
                fechaHoraInicio, fechaHoraFin, citaId);
        if (!citasSolapadas.isEmpty()) {
            throw new IllegalStateException("El trabajador no está disponible en el nuevo horario seleccionado.");
        }

        citaExistente.setTrabajador(trabajador);
        citaExistente.setServicio(servicio);
        citaExistente.setFechaHoraInicio(fechaHoraInicio);
        citaExistente.setFechaHoraFin(fechaHoraFin);

        Cita citaActualizada = repositorioCita.save(citaExistente);
        return convertToDto(citaActualizada);
    }

    public void deleteCita(Long citaId) {
        if (!repositorioCita.existsById(citaId)) {
            throw new ResourceNotFoundException("Cita no encontrada con id: " + citaId);
        }
        repositorioCita.deleteById(citaId);
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

    private DiaSemana mapToDiaSemana(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY:
                return DiaSemana.LUNES;
            case TUESDAY:
                return DiaSemana.MARTES;
            case WEDNESDAY:
                return DiaSemana.MIERCOLES;
            case THURSDAY:
                return DiaSemana.JUEVES;
            case FRIDAY:
                return DiaSemana.VIERNES;
            case SATURDAY:
                return DiaSemana.SABADO;
            case SUNDAY:
                return DiaSemana.DOMINGO;
            default:
                throw new IllegalArgumentException("Día de la semana no válido: " + dayOfWeek);
        }
    }

    private List<LocalTime[]> parseTimeIntervals(String intervalsString) {
        List<LocalTime[]> intervals = new ArrayList<>();
        if (intervalsString == null || intervalsString.trim().isEmpty()) {
            return intervals;
        }
        for (String part : intervalsString.split(",")) {
            String[] times = part.split("-");
            if (times.length == 2) {
                intervals.add(new LocalTime[]{LocalTime.parse(times[0]), LocalTime.parse(times[1])});
            }
        }
        return intervals;
    }

    public List<String> getHorariosDisponibles(Long trabajadorId, Long servicioId, LocalDate fecha) {
        repositorioTrabajador.findById(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));
        ServicioLocal servicio = repositorioServicios.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));

        final int DURACION_SERVICIO = servicio.getDuracionEnMinutos();
        final int INTERVALO_MINUTOS = 15; // Comprobamos la disponibilidad cada 15 minutos

        // 1. Obtener los intervalos de trabajo REALES del día
        List<LocalTime[]> workingIntervals = getWorkingIntervalsForDay(trabajadorId, fecha);
        if (workingIntervals.isEmpty()) {
            return new ArrayList<>(); // Si no hay horario de trabajo, no hay disponibilidad
        }

        // 2. Obtener citas y bloqueos existentes
        LocalDateTime inicioDelDia = fecha.atStartOfDay();
        LocalDateTime finDelDia = fecha.atTime(LocalTime.MAX);
        List<Cita> citasDelDia = repositorioCita.findCitasSolapadas(trabajadorId, inicioDelDia, finDelDia);
        List<PeriodoNoDisponible> bloqueosDelDia = new ArrayList<>();
        bloqueosDelDia.addAll(periodoNoDisponibleRepository.findByTipoAndFechaHoraInicioBeforeAndFechaHoraFinAfter(TipoBloqueo.GLOBAL, finDelDia, inicioDelDia));
        bloqueosDelDia.addAll(periodoNoDisponibleRepository.findByTrabajadorIdAndFechaHoraInicioBeforeAndFechaHoraFinAfter(trabajadorId, finDelDia, inicioDelDia));

        // 3. Generar y filtrar slots
        List<String> horariosDisponibles = new ArrayList<>();
        for (LocalTime[] interval : workingIntervals) {
            LocalTime horaActual = interval[0];
            LocalTime finIntervalo = interval[1];

            while (!horaActual.plusMinutes(DURACION_SERVICIO).isAfter(finIntervalo)) {
                LocalDateTime posibleInicio = fecha.atTime(horaActual);
                LocalDateTime posibleFin = posibleInicio.plusMinutes(DURACION_SERVICIO);

                boolean estaSolapado = isOverlapping(posibleInicio, posibleFin, citasDelDia, bloqueosDelDia);

                if (!estaSolapado) {
                    horariosDisponibles.add(horaActual.format(DateTimeFormatter.ofPattern("HH:mm")));
                }

                horaActual = horaActual.plusMinutes(INTERVALO_MINUTOS);
            }
        }
        return horariosDisponibles;
    }

    private List<LocalTime[]> getWorkingIntervalsForDay(Long trabajadorId, LocalDate fecha) {
        // Obtener el día de la semana
        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        DiaSemana diaSemana = mapToDiaSemana(dayOfWeek);

        // Obtener horario del negocio desde application.properties
        String businessHoursStr = env.getProperty("horario.negocio." + diaSemana.name().toLowerCase());
        List<LocalTime[]> businessIntervals = parseTimeIntervals(businessHoursStr);

        // Obtener turnos del trabajador desde la BD
        List<Turno> turnos = turnoRepository.findByTrabajadorIdAndDiaDeLaSemana(trabajadorId, diaSemana);
        List<LocalTime[]> workerIntervals = turnos.stream()
                .map(t -> new LocalTime[]{t.getHoraInicio(), t.getHoraFin()})
                .collect(Collectors.toList());

        // Calcular la intersección entre el horario del negocio y los turnos del trabajador
        List<LocalTime[]> finalIntervals = new ArrayList<>();
        for (LocalTime[] businessInt : businessIntervals) {
            for (LocalTime[] workerInt : workerIntervals) {
                LocalTime start = businessInt[0].isAfter(workerInt[0]) ? businessInt[0] : workerInt[0];
                LocalTime end = businessInt[1].isBefore(workerInt[1]) ? businessInt[1] : workerInt[1];

                if (!start.isAfter(end) && !start.equals(end)) {
                    finalIntervals.add(new LocalTime[]{start, end});
                }
            }
        }
        return finalIntervals;
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
        YearMonth yearMonth = YearMonth.of(anio, mes);
        List<LocalDate> diasNoDisponibles = new ArrayList<>();

        // Iteramos por cada día del mes
        for (int dia = 1; dia <= yearMonth.lengthOfMonth(); dia++) {
            LocalDate fechaActual = yearMonth.atDay(dia);

            // Si el día es pasado, lo añadimos directamente
            if (fechaActual.isBefore(LocalDate.now())) {
                diasNoDisponibles.add(fechaActual);
            } else if (getHorariosDisponibles(trabajadorId, servicioId, fechaActual).isEmpty()) {
                diasNoDisponibles.add(fechaActual);
            }
        }
        return diasNoDisponibles;
    }

    public List<CitasPorDiaDto> getCitasUltimaSemana() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime fechaInicio = hoy.minusDays(6).atStartOfDay(); // Desde hace 6 días hasta hoy (7 días en total)

        // 1. Obtener los conteos de la base de datos
        List<Map<String, Object>> resultadosDb = repositorioCita.countCitasFromDate(fechaInicio);

        // 2. Convertir los resultados a un mapa para fácil acceso: Map<LocalDate, Long>
        Map<LocalDate, Long> conteoPorFecha = resultadosDb.stream()
                .collect(Collectors.toMap(
                        // La base de datos devuelve java.sql.Date, lo convertimos a LocalDate
                        r -> ((java.sql.Date) r.get("fecha")).toLocalDate(),
                        r -> (Long) r.get("cantidad")));

        // 3. Generar la lista para los últimos 7 días, rellenando con 0 si no hay citas
        return IntStream.range(0, 7)
                .mapToObj(i -> hoy.minusDays(i)) // Iterar desde hoy hacia atrás 6 días
                .map(fecha -> {
                    // Formatear el nombre del día (ej: "Lun", "Mar")
                    String diaSemana = fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("es", "ES"));
                    long cantidad = conteoPorFecha.getOrDefault(fecha, 0L);
                    return new CitasPorDiaDto(diaSemana, cantidad);
                })
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            java.util.Collections.reverse(list); // Invertir para que sea cronológico
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