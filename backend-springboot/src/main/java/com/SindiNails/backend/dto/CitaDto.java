package com.SindiNails.backend.dto;

import java.time.LocalDateTime;

import com.SindiNails.backend.model.EstadoCita;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaDto {
    private Long id;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
    private ServicioResumenDto servicio;
    private TrabajadorDto trabajador;
    private UsuarioResumenDto cliente;
}