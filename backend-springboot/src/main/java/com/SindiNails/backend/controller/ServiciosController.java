package com.SindiNails.backend.controller;

import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.dto.ServicioDto;
import com.SindiNails.backend.repository.RepositorioServicios;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServiciosController {

    private final RepositorioServicios repositorioServicios;

    @GetMapping
    public List<ServicioDto> getServices(@RequestParam(required = false) Long tipoServicioId) {
        var servicios = tipoServicioId != null
                ? repositorioServicios.findByTipoServicioId(tipoServicioId)
                : repositorioServicios.findAll();
        return servicios.stream().map(ServicioDto::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDto> getServiceById(@PathVariable Long id) {
        ServicioDto servicio = repositorioServicios.findById(id)
                .map(ServicioDto::de)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        return ResponseEntity.ok(servicio);
    }
}
