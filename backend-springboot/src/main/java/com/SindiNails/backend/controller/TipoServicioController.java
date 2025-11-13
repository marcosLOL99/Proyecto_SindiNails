package com.SindiNails.backend.controller;

import com.SindiNails.backend.model.TipoServicio;
import com.SindiNails.backend.repository.RepositorioTipoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tiposervicios")
@CrossOrigin(origins = "http://localhost:5173")
public class TipoServicioController {

    @Autowired
    private RepositorioTipoServicio repositorioTipoServicio;

    @GetMapping
    public List<TipoServicio> getAllTiposServicio() { return repositorioTipoServicio.findAll(); }
}