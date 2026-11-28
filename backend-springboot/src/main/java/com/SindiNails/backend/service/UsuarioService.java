package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.UsuarioDto;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioCita;
import com.SindiNails.backend.repository.RepositorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioCita repositorioCita;

    public List<UsuarioDto> getAllClientes() {
        return repositorioUsuario.findAllByRole(Role.USER).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioDto updateUsuario(Long id, UsuarioDto usuarioDto) {
        Usuario usuario = buscarClienta(id);
        boolean emailDeOtraCuenta = repositorioUsuario.findByEmail(usuarioDto.getEmail())
                .filter(otra -> !otra.getId().equals(id))
                .isPresent();
        if (emailDeOtraCuenta) {
            throw new ReglaDeNegocioException("El email ya está en uso.");
        }

        usuario.setNombre(usuarioDto.getNombre());
        usuario.setApellidos(usuarioDto.getApellidos());
        usuario.setEmail(usuarioDto.getEmail());
        usuario.setTelefono(usuarioDto.getTelefono());

        Usuario usuarioActualizado;
        try {
            usuarioActualizado = repositorioUsuario.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Otra cuenta ha tomado ese email entre la comprobación y el guardado.
            throw new ReglaDeNegocioException("El email ya está en uso.");
        }
        return convertToDto(usuarioActualizado);
    }

    /** Sus citas (también las canceladas o pasadas) son el historial del salón: una clienta con citas no se borra. */
    public void deleteUsuario(Long id) {
        Usuario clienta = buscarClienta(id);
        if (repositorioCita.existsByClienteId(id)) {
            throw new ReglaDeNegocioException("No se puede borrar una clienta con citas.");
        }
        repositorioUsuario.delete(clienta);
    }

    /** Solo clientas: una administradora no se edita ni se borra desde la gestión de clientas (404, como si no existiera). */
    private Usuario buscarClienta(Long id) {
        return repositorioUsuario.findById(id)
                .filter(usuario -> usuario.getRole() == Role.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Clienta no encontrada con id: " + id));
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
