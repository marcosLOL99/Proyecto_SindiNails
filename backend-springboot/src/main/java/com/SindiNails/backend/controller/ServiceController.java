package com.SindiNails.backend.controller;

import com.SindiNails.backend.model.Service;
import com.SindiNails.backend.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:5173") // Permitimos peticiones desde el frontend de React
public class ServiceController {

    @Autowired
    private ServiceRepository serviceRepository;

    @GetMapping
    public List<Service> getAllServices() {
        // Para probar, si no hay datos, agregamos algunos.
        if (serviceRepository.count() == 0) {
            Service service1 = new Service();
            service1.setName("Manicura Clásica");
            service1.setDescription("Limado, cutículas y esmaltado tradicional.");
            service1.setPrice(15.0);
            service1.setDurationInMinutes(30);
            serviceRepository.save(service1);

            Service service2 = new Service();
            service2.setName("Uñas Acrílicas");
            service2.setDescription("Extensión de uñas con acrílico y esmaltado semipermanente.");
            service2.setPrice(40.0);
            service2.setDurationInMinutes(90);
            serviceRepository.save(service2);
        }
        return serviceRepository.findAll();
    }
}
