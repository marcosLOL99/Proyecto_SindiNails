package com.SindiNails.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "servicio_local")
public class ServicioLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String descripcion;

    private double precio;

    private int duracionEnMinutos;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_servicio_id")
    private TipoServicio tipoServicio;
}
