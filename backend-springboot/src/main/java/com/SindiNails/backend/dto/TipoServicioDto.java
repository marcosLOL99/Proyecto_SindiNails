package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.TipoServicio;

public record TipoServicioDto(Long id, String nombre) {

    public static TipoServicioDto de(TipoServicio tipo) {
        return new TipoServicioDto(tipo.getId(), tipo.getNombre());
    }
}
