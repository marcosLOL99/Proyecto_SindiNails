package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.DiaSemana;
import lombok.Data;
import java.time.LocalTime;

@Data
public class TurnoDto {
    private Long id;
    private Long trabajadorId;
    private String trabajadorNombre;
    private DiaSemana diaDeLaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}