package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.TrabajadorDto;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private final RepositorioTrabajador repositorioTrabajador;

    public List<TrabajadorDto> getTrabajadoresActivos() {
        List<Trabajador> trabajadores = repositorioTrabajador.findAllByActivoTrue();
        return trabajadores.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private TrabajadorDto convertToDto(Trabajador trabajador) {
        return TrabajadorDto.builder()
                .id(trabajador.getId())
                .nombre(trabajador.getNombre())
                .apellidos(trabajador.getApellidos())
                .build();
    }
}