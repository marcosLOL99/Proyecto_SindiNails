package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CreateUpdateServicioDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.TipoServicio;
import com.SindiNails.backend.repository.RepositorioServicios;
import com.SindiNails.backend.repository.RepositorioTipoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final RepositorioServicios repositorioServicios;
    private final RepositorioTipoServicio repositorioTipoServicio;

    @Transactional
    public ServicioLocal createServicio(CreateUpdateServicioDto servicioDto) {
        TipoServicio tipoServicio = repositorioTipoServicio.findById(servicioDto.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con id: " + servicioDto.getTipoServicioId()));

        ServicioLocal nuevoServicio = new ServicioLocal();
        nuevoServicio.setNombre(servicioDto.getNombre());
        nuevoServicio.setDescripcion(servicioDto.getDescripcion());
        nuevoServicio.setPrecio(servicioDto.getPrecio());
        nuevoServicio.setDuracionEnMinutos(servicioDto.getDuracionEnMinutos());
        nuevoServicio.setTipoServicio(tipoServicio);

        return repositorioServicios.save(nuevoServicio);
    }

    @Transactional
    public ServicioLocal updateServicio(Long id, CreateUpdateServicioDto servicioDto) {
        ServicioLocal servicioExistente = repositorioServicios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        TipoServicio tipoServicio = repositorioTipoServicio.findById(servicioDto.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con id: " + servicioDto.getTipoServicioId()));

        servicioExistente.setNombre(servicioDto.getNombre());
        servicioExistente.setDescripcion(servicioDto.getDescripcion());
        servicioExistente.setPrecio(servicioDto.getPrecio());
        servicioExistente.setDuracionEnMinutos(servicioDto.getDuracionEnMinutos());
        servicioExistente.setTipoServicio(tipoServicio);

        return repositorioServicios.save(servicioExistente);
    }

    public void deleteServicio(Long id) {
        if (!repositorioServicios.existsById(id)) {
            throw new ResourceNotFoundException("Servicio no encontrado con id: " + id);
        }
        repositorioServicios.deleteById(id);
    }
}
