package com.SindiNails.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUpdateTrabajadorDto {

    @NotBlank(message = "El nombre no puede estar vacío.")
    private String nombre;

    @NotBlank(message = "Los apellidos no pueden estar vacíos.")
    private String apellidos;

    @NotNull(message = "El estado de actividad no puede ser nulo.")
    private Boolean activo;
}
