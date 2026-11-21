package com.SindiNails.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegistroDto {
    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios.")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El email no es válido.")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio.")
    private String telefono;
}
