package com.SindiNails.backend.dto;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCitaDto {

    @NotNull(message = "El ID del servicio no puede ser nulo.")
    private Long idServicio;

    @NotNull(message = "El ID del trabajador no puede ser nulo.")
    private Long idTrabajador;

    @NotNull(message = "La fecha y hora de inicio no pueden ser nulas.")
    @Future(message = "La fecha de la cita debe ser en el futuro.")
    private LocalDateTime fechaHoraInicio;
}