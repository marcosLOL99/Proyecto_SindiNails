package com.SindiNails.backend.support;

import com.SindiNails.backend.model.*;
import com.SindiNails.backend.repository.*;
import org.springframework.boot.test.context.TestComponent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Crea y guarda datos de prueba con valores por defecto razonables.
 * Uso: {@code @Import(DatosDePrueba.class)} y {@code @Autowired DatosDePrueba datos}.
 */
@TestComponent
public class DatosDePrueba {

    private final RepositorioTipoServicio tipos;
    private final RepositorioServicios servicios;
    private final RepositorioTrabajador trabajadores;
    private final RepositorioUsuario usuarios;
    private final RepositorioTurno turnos;
    private final RopositorioPeriodoNoDisp periodos;
    private final RepositorioCita citas;

    public DatosDePrueba(RepositorioTipoServicio tipos, RepositorioServicios servicios,
                         RepositorioTrabajador trabajadores, RepositorioUsuario usuarios,
                         RepositorioTurno turnos, RopositorioPeriodoNoDisp periodos, RepositorioCita citas) {
        this.tipos = tipos;
        this.servicios = servicios;
        this.trabajadores = trabajadores;
        this.usuarios = usuarios;
        this.turnos = turnos;
        this.periodos = periodos;
        this.citas = citas;
    }

    public ServicioLocal servicio(String nombre, int minutos) {
        TipoServicio tipo = new TipoServicio();
        tipo.setNombre("Manicura");
        tipos.save(tipo);
        ServicioLocal s = new ServicioLocal();
        s.setNombre(nombre);
        s.setDescripcion(nombre);
        s.setPrecio(20.0);
        s.setDuracionEnMinutos(minutos);
        s.setTipoServicio(tipo);
        return servicios.save(s);
    }

    public Trabajador trabajadora(String nombre) {
        return trabajadores.save(Trabajador.builder().nombre(nombre).apellidos("García").build());
    }

    public Trabajador trabajadoraInactiva(String nombre) {
        return trabajadores.save(Trabajador.builder().nombre(nombre).apellidos("García").activo(false).build());
    }

    public Usuario clienta(String nombre) {
        return usuarios.save(Usuario.builder()
                .nombre(nombre).apellidos("Pérez")
                .email(nombre.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .password("x").telefono("600000000").role(Role.USER).build());
    }

    public void turno(Trabajador t, LocalDate fecha, String inicio, String fin) {
        turnos.save(new Turno(null, t, fecha, LocalTime.parse(inicio), LocalTime.parse(fin)));
    }

    public void ausencia(Trabajador t, LocalDateTime inicio, LocalDateTime fin) {
        periodos.save(PeriodoNoDisponible.builder().tipo(TipoBloqueo.TRABAJADOR).trabajador(t)
                .motivo("Ausencia").fechaHoraInicio(inicio).fechaHoraFin(fin).build());
    }

    public void bloqueoGlobal(LocalDateTime inicio, LocalDateTime fin) {
        periodos.save(PeriodoNoDisponible.builder().tipo(TipoBloqueo.GLOBAL)
                .motivo("Cerrado").fechaHoraInicio(inicio).fechaHoraFin(fin).build());
    }

    public Cita cita(Usuario cliente, Trabajador t, ServicioLocal s, LocalDateTime inicio, EstadoCita estado) {
        return citas.save(Cita.builder().cliente(cliente).trabajador(t).servicio(s)
                .fechaHoraInicio(inicio).fechaHoraFin(inicio.plusMinutes(s.getDuracionEnMinutos()))
                .estado(estado).build());
    }
}
