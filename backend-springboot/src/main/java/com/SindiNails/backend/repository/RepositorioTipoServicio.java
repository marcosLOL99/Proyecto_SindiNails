package com.SindiNails.backend.repository;

import com.SindiNails.backend.model.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioTipoServicio extends JpaRepository<TipoServicio, Long> {}