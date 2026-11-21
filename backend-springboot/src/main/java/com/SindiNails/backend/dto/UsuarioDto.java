package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.Role;
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
    private String nombre;
    private String apellidos;
    private String email;
    @NotBlank(message = "El teléfono es obligatorio.")
    private String telefono;
    private Role role;
}
