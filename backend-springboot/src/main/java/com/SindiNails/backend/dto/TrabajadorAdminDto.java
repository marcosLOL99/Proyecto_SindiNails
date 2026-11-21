package com.SindiNails.backend.dto;

import com.SindiNails.backend.model.Trabajador;

/** Una trabajadora tal como la ve la administración (la vista pública es TrabajadorDto, sin "activo"). */
public record TrabajadorAdminDto(Long id, String nombre, String apellidos, boolean activo) {

    public static TrabajadorAdminDto de(Trabajador trabajador) {
        return new TrabajadorAdminDto(trabajador.getId(), trabajador.getNombre(), trabajador.getApellidos(),
                trabajador.isActivo());
    }
}
