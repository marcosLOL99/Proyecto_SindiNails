package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.UsuarioDto;
import com.SindiNails.backend.dto.EditarUsuarioDto;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getAuthenticatedUser(@AuthenticationPrincipal Usuario usuario) {
        UsuarioDto userDto = UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .role(usuario.getRole())
                .build();
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioDto> updateAuthenticatedUser(@AuthenticationPrincipal Usuario usuario, @Valid @RequestBody EditarUsuarioDto updateUserDto) {
        Usuario updatedUsuario = authService.editarUsuario(usuario, updateUserDto);
        UsuarioDto userDto = UsuarioDto.builder()
                .id(updatedUsuario.getId())
                .nombre(updatedUsuario.getNombre())
                .apellidos(updatedUsuario.getApellidos())
                .email(updatedUsuario.getEmail())
                .telefono(updatedUsuario.getTelefono())
                .role(updatedUsuario.getRole())
                .build();
        return ResponseEntity.ok(userDto);
    }
}