package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.TipoServicioDto;
import com.SindiNails.backend.service.ServicioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tiposervicios")
@RequiredArgsConstructor
public class TipoServicioController {

    private final ServicioService servicioService;

    @GetMapping
    public List<TipoServicioDto> getAllTiposServicio() {
        return servicioService.getTiposServicio();
    }
}