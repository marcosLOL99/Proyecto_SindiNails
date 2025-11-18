package com.SindiNails.backend.dto;

import lombok.Data;

@Data
public class RegistroDto {
    private String password;
    private String nombre;
    private String apellidos;
    private String email;
    private String telefono;
}
