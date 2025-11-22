package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.TrabajadorDto;
import com.SindiNails.backend.service.TrabajadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trabajadores")
@RequiredArgsConstructor
public class TrabajadorController {

    private final TrabajadorService trabajadorService;

    @GetMapping
    public ResponseEntity<List<TrabajadorDto>> getTrabajadoresActivos() {
        return ResponseEntity.ok(trabajadorService.getTrabajadoresActivos());
    }
}