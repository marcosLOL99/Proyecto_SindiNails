package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CitaDto;
import com.SindiNails.backend.dto.AdminCreateCitaDto;
import com.SindiNails.backend.dto.CreateCitaDto;
import com.SindiNails.backend.dto.ServicioResumenDto;
import com.SindiNails.backend.dto.TrabajadorDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Cita;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioServicios;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import com.SindiNails.backend.repository.RepositorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitaService {

    private final RepositorioCita repositorioCita;
    private final RepositorioTrabajador repositorioTrabajador;
    private final RepositorioServicios repositorioServicios;
    private final RepositorioUsuario repositorioUsuario;

    @Transactional
    public CitaDto createCita(CreateCitaDto createCitaDto, Usuario cliente) {
        ServicioLocal servicio = repositorioServicios.findById(createCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + createCitaDto.getIdServicio()));

        Trabajador trabajador = repositorioTrabajador.findById(createCitaDto.getIdTrabajador())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + createCitaDto.getIdTrabajador()));

        LocalDateTime fechaHoraInicio = createCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());

        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadas(trabajador.getId(), fechaHoraInicio, fechaHoraFin);
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
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + createCitaDto.getIdCliente()));

        return createCita(createCitaDto, cliente);
    }

    @Transactional
    public CitaDto updateCita(Long citaId, CreateCitaDto updateCitaDto) {
        Cita citaExistente = repositorioCita.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada con id: " + citaId));

        ServicioLocal servicio = repositorioServicios.findById(updateCitaDto.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + updateCitaDto.getIdServicio()));

        Trabajador trabajador = repositorioTrabajador.findById(updateCitaDto.getIdTrabajador())
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + updateCitaDto.getIdTrabajador()));

        LocalDateTime fechaHoraInicio = updateCitaDto.getFechaHoraInicio();
        LocalDateTime fechaHoraFin = fechaHoraInicio.plusMinutes(servicio.getDuracionEnMinutos());

        // Validar disponibilidad excluyendo la propia cita que se está actualizando
        List<Cita> citasSolapadas = repositorioCita.findCitasSolapadasExcluyendoCita(trabajador.getId(), fechaHoraInicio, fechaHoraFin, citaId);
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

    public List<String> getHorariosDisponibles(Long trabajadorId, Long servicioId, LocalDate fecha) {
        // Validar que el trabajador y el servicio existen
        repositorioTrabajador.findById(trabajadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + trabajadorId));
        ServicioLocal servicio = repositorioServicios.findById(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servicioId));

        // Definir horario laboral (se puede hacer más complejo en el futuro)
        final LocalTime HORA_INICIO_JORNADA = LocalTime.of(9, 0);
        final LocalTime HORA_FIN_JORNADA = LocalTime.of(20, 0);
        final int DURACION_SERVICIO = servicio.getDuracionEnMinutos();
        final int INTERVALO_MINUTOS = 15; // Comprobamos la disponibilidad cada 15 minutos

        // Obtener las citas ya existentes para ese trabajador en esa fecha
        LocalDateTime inicioDelDia = fecha.atStartOfDay();
        LocalDateTime finDelDia = fecha.atTime(LocalTime.MAX);
        List<Cita> citasDelDia = repositorioCita.findCitasSolapadas(trabajadorId, inicioDelDia, finDelDia);

        List<String> horariosDisponibles = new ArrayList<>();
        LocalTime horaActual = HORA_INICIO_JORNADA;

        while (horaActual.plusMinutes(DURACION_SERVICIO).isBefore(HORA_FIN_JORNADA) || horaActual.plusMinutes(DURACION_SERVICIO).equals(HORA_FIN_JORNADA)) {
            LocalDateTime posibleInicio = fecha.atTime(horaActual);
            LocalDateTime posibleFin = posibleInicio.plusMinutes(DURACION_SERVICIO);

            boolean estaSolapado = false;
            for (Cita citaExistente : citasDelDia) {
                // Comprobamos si el nuevo hueco se solapa con una cita existente
                if (posibleInicio.isBefore(citaExistente.getFechaHoraFin()) && posibleFin.isAfter(citaExistente.getFechaHoraInicio())) {
                    estaSolapado = true;
                    break;
                }
            }

            if (!estaSolapado) {
                // Si no hay solapamiento, añadimos la hora a la lista de disponibles
                horariosDisponibles.add(horaActual.format(DateTimeFormatter.ofPattern("HH:mm")));
            }

            // Avanzamos al siguiente intervalo
            horaActual = horaActual.plusMinutes(INTERVALO_MINUTOS);
        }
        return horariosDisponibles;
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

        return CitaDto.builder()
                .id(cita.getId())
                .fechaHoraInicio(cita.getFechaHoraInicio())
                .fechaHoraFin(cita.getFechaHoraFin())
                .estado(cita.getEstado())
                .servicio(servicioDto)
                .trabajador(trabajadorDto)
                .build();
    }
}