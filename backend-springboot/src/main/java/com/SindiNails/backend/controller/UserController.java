package com.SindiNails.backend.controller;

import com.SindiNails.backend.dto.UsuarioDto;
import com.SindiNails.backend.model.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UsuarioDto> getAuthenticatedUser(@AuthenticationPrincipal Usuario usuario) {
        UsuarioDto userDto = UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .build();
        return ResponseEntity.ok(userDto);
    }
}
