package com.SindiNails.backend.dto;

import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDate;


@Data
public class TurnoDto {
    private Long id;
    private Long trabajadorId;
    private String trabajadorNombre;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}