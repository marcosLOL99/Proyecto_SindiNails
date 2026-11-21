package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.ServicioDto;
import com.SindiNails.backend.service.ServicioService;
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

    private final ServicioService servicioService;

    @GetMapping
    public List<ServicioDto> getServices(@RequestParam(required = false) Long tipoServicioId) {
        return servicioService.getServicios(tipoServicioId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioDto> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.getServicio(id));
    }
}
