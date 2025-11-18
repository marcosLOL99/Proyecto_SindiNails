package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.AuthResponseDto;
import com.SindiNails.backend.dto.LoginDto;
import com.SindiNails.backend.dto.RegistroDto;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final RepositorioUsuario repositorioUsuario;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponseDto register(RegistroDto request) {
        if (repositorioUsuario.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está en uso.");
        }

        var user = Usuario.builder()
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .email(request.getEmail())
                .telefono(request.getTelefono())
                .role(Role.USER)
                .build();
        repositorioUsuario.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthResponseDto.builder().token(jwtToken).build();
    }

    public AuthResponseDto login(LoginDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var user = repositorioUsuario.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return AuthResponseDto.builder().token(jwtToken).build();
    }
}