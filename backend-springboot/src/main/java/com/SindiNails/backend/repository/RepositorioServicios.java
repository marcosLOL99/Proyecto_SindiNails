package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.ServicioLocal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioServicios extends JpaRepository<ServicioLocal, Long> {

    List<ServicioLocal> findByTipoServicioId(Long tipoServicioId);
}
