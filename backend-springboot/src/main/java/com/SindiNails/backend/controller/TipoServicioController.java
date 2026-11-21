package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.TipoServicioDto;
import com.SindiNails.backend.repository.RepositorioTipoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tiposervicios")
public class TipoServicioController {

    @Autowired
    private RepositorioTipoServicio repositorioTipoServicio;

    @GetMapping
    public List<TipoServicioDto> getAllTiposServicio() {
        return repositorioTipoServicio.findAll().stream().map(TipoServicioDto::de).toList();
    }
}