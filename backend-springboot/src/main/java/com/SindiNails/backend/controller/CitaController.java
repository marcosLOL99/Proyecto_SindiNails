package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.CitaDto;
import com.SindiNails.backend.dto.CreateCitaDto;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.service.CitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    public ResponseEntity<CitaDto> createCita(@Valid @RequestBody CreateCitaDto createCitaDto, @AuthenticationPrincipal Usuario cliente) {
        CitaDto nuevaCita = citaService.createCita(createCitaDto, cliente);
        return new ResponseEntity<>(nuevaCita, HttpStatus.CREATED);
    }

    @GetMapping("/mis-citas")
    public ResponseEntity<List<CitaDto>> getMisCitas(@AuthenticationPrincipal Usuario cliente) {
        List<CitaDto> misCitas = citaService.getMisCitas(cliente);
        return ResponseEntity.ok(misCitas);
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<String>> getHorariosDisponibles(
            @RequestParam Long trabajadorId,
            @RequestParam Long servicioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        List<String> horarios = citaService.getHorariosDisponibles(trabajadorId, servicioId, fecha);
        return ResponseEntity.ok(horarios);
    }
}