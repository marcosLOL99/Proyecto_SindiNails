package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDto {
    private Long id;
    @NotBlank(message = "El nombre es obligatorio.")
    private String nombre;
    @NotBlank(message = "Los apellidos son obligatorios.")
    private String apellidos;
    @NotBlank(message = "El email es obligatorio.")
    @Email(message = "El email no es válido.")
    private String email;
    @NotBlank(message = "El teléfono es obligatorio.")
    private String telefono;
    private Role role;
}
