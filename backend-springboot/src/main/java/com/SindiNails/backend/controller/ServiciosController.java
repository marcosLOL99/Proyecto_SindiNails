package com.SindiNails.backend.controller;

import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.ServicioLocal;
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
    public List<ServicioLocal> getServices(@RequestParam(required = false) Long tipoServicioId) {
        if (tipoServicioId != null) {
            return repositorioServicios.findByTipoServicioId(tipoServicioId);
        } else {
            return repositorioServicios.findAll();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioLocal> getServiceById(@PathVariable Long id) {
        ServicioLocal servicio = repositorioServicios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
        return ResponseEntity.ok(servicio);
    }
}
