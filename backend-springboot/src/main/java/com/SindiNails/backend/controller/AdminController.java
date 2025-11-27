package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.BloqueoDto;
import com.SindiNails.backend.dto.CitaDto;
import com.SindiNails.backend.dto.CitasPorDiaDto;
import com.SindiNails.backend.dto.AdminCreateCitaDto;
import com.SindiNails.backend.dto.UsuarioDto;
import com.SindiNails.backend.dto.CreateUpdateServicioDto;
import com.SindiNails.backend.dto.CreateUpdateTrabajadorDto;
import com.SindiNails.backend.dto.CreateCitaDto;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.Trabajador;
import com.SindiNails.backend.service.CitaService;
import com.SindiNails.backend.service.UsuarioService;
import com.SindiNails.backend.service.ServicioService;
import com.SindiNails.backend.service.PeriodoNoDisponibleService;
import com.SindiNails.backend.service.TrabajadorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    // Inyectamos los servicios necesarios
    private final CitaService citaService;
    private final ServicioService servicioService;
    private final TrabajadorService trabajadorService;
    private final UsuarioService usuarioService;
    private final PeriodoNoDisponibleService periodoNoDisponibleService;

    // --- Endpoints para Citas ---
    @GetMapping("/citas")
    public ResponseEntity<List<CitaDto>> getAllCitas() {
        List<CitaDto> todasLasCitas = citaService.getAllCitas();
        return ResponseEntity.ok(todasLasCitas);
    }

    @PostMapping("/citas")
    public ResponseEntity<CitaDto> createCitaByAdmin(@Valid @RequestBody AdminCreateCitaDto createCitaDto) {
        CitaDto nuevaCita = citaService.createCitaByAdmin(createCitaDto);
        return new ResponseEntity<>(nuevaCita, HttpStatus.CREATED);
    }

    @GetMapping("/stats/citas-semanales")
    public ResponseEntity<List<CitasPorDiaDto>> getCitasSemanales() {
        List<CitasPorDiaDto> stats = citaService.getCitasUltimaSemana();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/citas/{id}")
    public ResponseEntity<CitaDto> updateCita(@PathVariable Long id, @Valid @RequestBody CreateCitaDto updateCitaDto) {
        CitaDto citaActualizada = citaService.updateCita(id, updateCitaDto);
        return ResponseEntity.ok(citaActualizada);
    }

    @DeleteMapping("/citas/{id}")
    public ResponseEntity<Void> deleteCita(@PathVariable Long id) {
        citaService.deleteCita(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints para Servicios ---

    @PostMapping("/servicios")
    public ResponseEntity<ServicioLocal> createServicio(@Valid @RequestBody CreateUpdateServicioDto servicioDto) {
        ServicioLocal nuevoServicio = servicioService.createServicio(servicioDto);
        return new ResponseEntity<>(nuevoServicio, HttpStatus.CREATED);
    }

    @PutMapping("/servicios/{id}")
    public ResponseEntity<ServicioLocal> updateServicio(@PathVariable Long id, @Valid @RequestBody CreateUpdateServicioDto servicioDto) {
        ServicioLocal servicioActualizado = servicioService.updateServicio(id, servicioDto);
        return ResponseEntity.ok(servicioActualizado);
    }

    @DeleteMapping("/servicios/{id}")
    public ResponseEntity<Void> deleteServicio(@PathVariable Long id) {
        servicioService.deleteServicio(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints para Trabajadores ---

    @GetMapping("/trabajadores")
    public ResponseEntity<List<Trabajador>> getAllTrabajadores() {
        List<Trabajador> trabajadores = trabajadorService.getAllTrabajadores();
        return ResponseEntity.ok(trabajadores);
    }

    @PostMapping("/trabajadores")
    public ResponseEntity<Trabajador> createTrabajador(@Valid @RequestBody CreateUpdateTrabajadorDto trabajadorDto) {
        Trabajador nuevoTrabajador = trabajadorService.createTrabajador(trabajadorDto);
        return new ResponseEntity<>(nuevoTrabajador, HttpStatus.CREATED);
    }

    @PutMapping("/trabajadores/{id}")
    public ResponseEntity<Trabajador> updateTrabajador(@PathVariable Long id, @Valid @RequestBody CreateUpdateTrabajadorDto trabajadorDto) {
        Trabajador trabajadorActualizado = trabajadorService.updateTrabajador(id, trabajadorDto);
        return ResponseEntity.ok(trabajadorActualizado);
    }

    @DeleteMapping("/trabajadores/{id}")
    public ResponseEntity<Void> deleteTrabajador(@PathVariable Long id) {
        trabajadorService.deleteTrabajador(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints para Clientes (Usuarios) ---

    @GetMapping("/clientes")
    public ResponseEntity<List<UsuarioDto>> getAllClientes() {
        List<UsuarioDto> clientes = usuarioService.getAllClientes();
        return ResponseEntity.ok(clientes);
    }

    @PutMapping("/clientes/{id}")
    public ResponseEntity<UsuarioDto> updateUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDto usuarioDto) {
        UsuarioDto usuarioActualizado = usuarioService.updateUsuario(id, usuarioDto);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/clientes/{id}")
    public ResponseEntity<Void> deleteUsuario(@PathVariable Long id) {
        usuarioService.deleteUsuario(id);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints para Periodos No Disponibles (Bloqueos) ---

    @PostMapping("/bloqueos")
    public ResponseEntity<BloqueoDto> crearBloqueo(@Valid @RequestBody BloqueoDto bloqueoDto) {
        BloqueoDto nuevoBloqueo = periodoNoDisponibleService.crearBloqueo(bloqueoDto);
        return new ResponseEntity<>(nuevoBloqueo, HttpStatus.CREATED);
    }

    @GetMapping("/bloqueos/trabajador/{trabajadorId}")
    public ResponseEntity<List<BloqueoDto>> getBloqueosPorTrabajador(@PathVariable Long trabajadorId) {
        List<BloqueoDto> bloqueos = periodoNoDisponibleService.getBloqueosPorTrabajador(trabajadorId);
        return ResponseEntity.ok(bloqueos);
    }

    @GetMapping("/bloqueos/globales")
    public ResponseEntity<List<BloqueoDto>> getBloqueosGlobales() {
        return ResponseEntity.ok(periodoNoDisponibleService.getBloqueosGlobales());
    }

    @DeleteMapping("/bloqueos/{id}")
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable Long id) {
        periodoNoDisponibleService.eliminarBloqueo(id);
        return ResponseEntity.noContent().build();
    }
}
