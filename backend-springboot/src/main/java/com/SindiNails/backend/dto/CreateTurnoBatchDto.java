package com.SindiNails.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Data
public class CreateTurnoBatchDto {
    @NotNull(message = "El ID del trabajador es obligatorio.")
    private Long trabajadorId;

    @NotNull(message = "La fecha de inicio de la semana es obligatoria.")
    private LocalDate fechaInicioSemana;

    /** Clave: día de la semana, de 0 (lunes) a 6 (domingo). */
    @NotNull(message = "Los horarios son obligatorios.")
    private Map<Integer, HorarioDia> horarios;

    @Data
    public static class HorarioDia {
        private boolean activo;
        private LocalTime horaInicio;
        private LocalTime horaFin;
        private LocalTime horaInicioDescanso;
        private LocalTime horaFinDescanso;
    }
}