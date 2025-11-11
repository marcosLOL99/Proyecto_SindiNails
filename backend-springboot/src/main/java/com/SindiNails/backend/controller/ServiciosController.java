package com.SindiNails.backend.controller;

import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.repository.RepositorioServicios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@CrossOrigin(origins = "http://localhost:5173")
public class ServiciosController {

    @Autowired
    private RepositorioServicios RepositorioServicios;

    @GetMapping
    public List<ServicioLocal> getAllServices() {
        if (RepositorioServicios.count() == 0) {
            ServicioLocal servicio1 = new ServicioLocal();
            servicio1.setNombre("Manicura Clásica");
            servicio1.setDescripcion("Limado, cutículas y esmaltado tradicional.");
            servicio1.setPrecio(15.0);
            servicio1.setDuracionEnMinutos(30);
            RepositorioServicios.save(servicio1);

            ServicioLocal servicio2 = new ServicioLocal();
            servicio2.setNombre("Uñas Acrílicas");
            servicio2.setDescripcion("Extensión de uñas con acrílico y esmaltado semipermanente.");
            servicio2.setPrecio(40.0);
            servicio2.setDuracionEnMinutos(90);
            RepositorioServicios.save(servicio2);
        }
        return RepositorioServicios.findAll();
    }
}
