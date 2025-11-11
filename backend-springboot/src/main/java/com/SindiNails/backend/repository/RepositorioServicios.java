package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.ServicioLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioServicios extends JpaRepository<ServicioLocal, Long> {
}
