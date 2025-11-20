package com.SindiNails.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditarUsuarioDto {
    private String nombre;
    private String apellidos;
    private String telefono;
}

