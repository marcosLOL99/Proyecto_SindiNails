package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.TipoBloqueo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BloqueoDto {
    private Long id;

    @NotNull(message = "La fecha y hora de inicio son obligatorias.")
    private LocalDateTime fechaHoraInicio;

    @NotNull(message = "La fecha y hora de fin son obligatorias.")
    private LocalDateTime fechaHoraFin;

    @NotBlank(message = "El motivo no puede estar vacío.")
    private String motivo;

    @NotNull
    private TipoBloqueo tipo;

    private Long trabajadorId;
}

