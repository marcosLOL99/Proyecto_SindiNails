package com.SindiNails.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUpdateServicioDto {
    @NotBlank(message = "El nombre no puede estar vacío.")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El precio no puede ser nulo.")
    @Min(value = 0, message = "El precio no puede ser negativo.")
    private Double precio;

    @NotNull(message = "La duración no puede ser nula.")
    @Min(value = 1, message = "La duración debe ser de al menos 1 minuto.")
    private Integer duracionEnMinutos;

    @NotNull(message = "El ID del tipo de servicio no puede ser nulo.")
    private Long tipoServicioId;
}
