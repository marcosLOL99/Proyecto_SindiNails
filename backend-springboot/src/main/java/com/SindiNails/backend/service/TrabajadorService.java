package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CreateUpdateTrabajadorDto;
import com.SindiNails.backend.dto.TrabajadorDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.repository.RepositorioTrabajador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<Trabajador> getAllTrabajadores() {
        return repositorioTrabajador.findAll();
    }

    @Transactional
    public Trabajador createTrabajador(CreateUpdateTrabajadorDto trabajadorDto) {
        Trabajador nuevoTrabajador = Trabajador.builder()
                .nombre(trabajadorDto.getNombre())
                .apellidos(trabajadorDto.getApellidos())
                .activo(trabajadorDto.getActivo())
                .build();
        return repositorioTrabajador.save(nuevoTrabajador);
    }

    @Transactional
    public Trabajador updateTrabajador(Long id, CreateUpdateTrabajadorDto trabajadorDto) {
        Trabajador trabajadorExistente = repositorioTrabajador.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));

        trabajadorExistente.setNombre(trabajadorDto.getNombre());
        trabajadorExistente.setApellidos(trabajadorDto.getApellidos());
        trabajadorExistente.setActivo(trabajadorDto.getActivo());

        return repositorioTrabajador.save(trabajadorExistente);
    }

    @Transactional
    public void deleteTrabajador(Long id) {
        Trabajador trabajador = repositorioTrabajador.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trabajador no encontrado con id: " + id));

        trabajador.setActivo(false);
        repositorioTrabajador.save(trabajador);
    }

    private TrabajadorDto convertToDto(Trabajador trabajador) {
        return TrabajadorDto.builder()
                .id(trabajador.getId())
                .nombre(trabajador.getNombre())
                .apellidos(trabajador.getApellidos())
                .build();
    }
}