package com.SindiNails.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioResumenDto {
    private Long id;
    private String nombre;
    private double precio;
    private int duracionEnMinutos;
}