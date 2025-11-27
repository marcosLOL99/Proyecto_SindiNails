package com.SindiNails.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Data
public class CreateTurnoBatchDto {
    @NotNull
    private Long trabajadorId;

    @NotNull
    private LocalDate fechaInicioSemana; // Lunes de la semana seleccionada

    // Un mapa donde la clave es el día de la semana (0=Lunes, 1=Martes...)
    // y el valor es el horario para ese día.
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