package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.DiaSemana;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CreateUpdateTurnoDto {
    @NotNull(message = "El ID del trabajador es obligatorio.")
    private Long trabajadorId;

    @NotNull(message = "El día de la semana es obligatorio.")
    private DiaSemana diaDeLaSemana;

    @NotNull(message = "La hora de inicio es obligatoria.")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria.")
    private LocalTime horaFin;
}