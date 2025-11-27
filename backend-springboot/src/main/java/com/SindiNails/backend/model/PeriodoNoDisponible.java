package com.SindiNails.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "periodos_no_disponibles")
public class PeriodoNoDisponible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoBloqueo tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trabajador_id") // Es nullable por defecto, lo que permite bloqueos globales sin trabajador.
    private Trabajador trabajador;
}