package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositorioUsuario extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);

    List<Usuario> findAllByRole(Role role);
}
