package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.ServicioLocal;

/** Un servicio del catálogo tal como lo ve el cliente de la API. */
public record ServicioDto(Long id, String nombre, String descripcion, double precio, int duracionEnMinutos,
                          TipoServicioDto tipoServicio) {

    public static ServicioDto de(ServicioLocal servicio) {
        return new ServicioDto(servicio.getId(), servicio.getNombre(), servicio.getDescripcion(),
                servicio.getPrecio(), servicio.getDuracionEnMinutos(),
                servicio.getTipoServicio() == null ? null : TipoServicioDto.de(servicio.getTipoServicio()));
    }
}
