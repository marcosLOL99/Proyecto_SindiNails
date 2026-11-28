package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.UsuarioDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final RepositorioUsuario repositorioUsuario;

    public List<UsuarioDto> getAllClientes() {
        return repositorioUsuario.findAllByRole(Role.USER).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioDto updateUsuario(Long id, UsuarioDto usuarioDto) {
        Usuario usuario = repositorioUsuario.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        usuario.setNombre(usuarioDto.getNombre());
        usuario.setApellidos(usuarioDto.getApellidos());
        usuario.setEmail(usuarioDto.getEmail());
        usuario.setTelefono(usuarioDto.getTelefono());

        Usuario usuarioActualizado = repositorioUsuario.save(usuario);
        return convertToDto(usuarioActualizado);
    }

    public void deleteUsuario(Long id) {
        if (!repositorioUsuario.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado con id: " + id);
        }
        repositorioUsuario.deleteById(id);
    }

    private UsuarioDto convertToDto(Usuario usuario) {
        return UsuarioDto.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellidos(usuario.getApellidos())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .role(usuario.getRole())
                .build();
    }
}
