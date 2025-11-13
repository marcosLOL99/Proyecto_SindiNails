package com.SindiNails.backend.controller;

import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.repository.RepositorioServicios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "http://localhost:5173")
public class ServiciosController {

    @Autowired
    private RepositorioServicios repositorioServicios;

    @GetMapping
    public List<ServicioLocal> getServices(@RequestParam(required = false) Long tipoServicioId) {
        if (tipoServicioId != null) {
            return repositorioServicios.findByTipoServicioId(tipoServicioId);
        } else {
            return repositorioServicios.findAll();
        }
    }
}
