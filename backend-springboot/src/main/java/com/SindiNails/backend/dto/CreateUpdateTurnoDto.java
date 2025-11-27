package com.SindiNails.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDate;

@Data
public class CreateUpdateTurnoDto {
    @NotNull(message = "El ID del trabajador es obligatorio.")
    private Long trabajadorId;

    @NotNull(message = "La fecha es obligatoria.")
    private LocalDate fecha;

    @NotNull(message = "La hora de inicio es obligatoria.")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria.")
    private LocalTime horaFin;
}