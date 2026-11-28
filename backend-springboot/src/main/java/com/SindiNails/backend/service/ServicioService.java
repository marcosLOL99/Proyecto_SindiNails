package com.SindiNails.backend.service;

import com.SindiNails.backend.dto.CreateUpdateServicioDto;
import com.SindiNails.backend.dto.ServicioDto;
import com.SindiNails.backend.dto.TipoServicioDto;
import com.SindiNails.backend.exception.ResourceNotFoundException;
import com.SindiNails.backend.model.ServicioLocal;
import com.SindiNails.backend.model.TipoServicio;
import com.SindiNails.backend.repository.RepositorioServicios;
import com.SindiNails.backend.repository.RepositorioTipoServicio;
import com.SindiNails.backend.exception.ReglaDeNegocioException;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final RepositorioServicios repositorioServicios;
    private final RepositorioTipoServicio repositorioTipoServicio;

    /** El catálogo completo, o solo los servicios de un tipo si se indica. */
    public List<ServicioDto> getServicios(Long tipoServicioId) {
        var servicios = tipoServicioId != null
                ? repositorioServicios.findByTipoServicioId(tipoServicioId)
                : repositorioServicios.findAll();
        return servicios.stream().map(ServicioDto::de).toList();
    }

    public ServicioDto getServicio(Long id) {
        return repositorioServicios.findById(id)
                .map(ServicioDto::de)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));
    }

    public List<TipoServicioDto> getTiposServicio() {
        return repositorioTipoServicio.findAll().stream().map(TipoServicioDto::de).toList();
    }

    @Transactional
    public ServicioDto createServicio(CreateUpdateServicioDto servicioDto) {
        TipoServicio tipoServicio = repositorioTipoServicio.findById(servicioDto.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con id: " + servicioDto.getTipoServicioId()));

        ServicioLocal nuevoServicio = new ServicioLocal();
        nuevoServicio.setNombre(servicioDto.getNombre());
        nuevoServicio.setDescripcion(servicioDto.getDescripcion());
        nuevoServicio.setPrecio(servicioDto.getPrecio());
        nuevoServicio.setDuracionEnMinutos(servicioDto.getDuracionEnMinutos());
        nuevoServicio.setTipoServicio(tipoServicio);

        return ServicioDto.de(repositorioServicios.save(nuevoServicio));
    }

    @Transactional
    public ServicioDto updateServicio(Long id, CreateUpdateServicioDto servicioDto) {
        ServicioLocal servicioExistente = repositorioServicios.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + id));

        TipoServicio tipoServicio = repositorioTipoServicio.findById(servicioDto.getTipoServicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de servicio no encontrado con id: " + servicioDto.getTipoServicioId()));

        servicioExistente.setNombre(servicioDto.getNombre());
        servicioExistente.setDescripcion(servicioDto.getDescripcion());
        servicioExistente.setPrecio(servicioDto.getPrecio());
        servicioExistente.setDuracionEnMinutos(servicioDto.getDuracionEnMinutos());
        servicioExistente.setTipoServicio(tipoServicio);

        return ServicioDto.de(repositorioServicios.save(servicioExistente));
    }

    public void deleteServicio(Long id) {
        if (!repositorioServicios.existsById(id)) {
            throw new ResourceNotFoundException("Servicio no encontrado con id: " + id);
        }
        try {
            repositorioServicios.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            // Sus citas (también las pasadas) son el historial del salón: la clave foránea impide borrarlo.
            throw new ReglaDeNegocioException("No se puede borrar un servicio con citas.");
        }
    }
}
