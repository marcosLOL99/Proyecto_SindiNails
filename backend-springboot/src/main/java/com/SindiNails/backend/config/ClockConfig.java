package com.SindiNails.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de la aplicación. Los servicios que dependen de "ahora" lo reciben inyectado
 * en vez de llamar a LocalDateTime.now(), así los tests pueden fijar la fecha y la hora.
 * Usa la zona horaria del salón (app.zona-horaria) y no la del sistema: en docker la JVM está en UTC,
 * y las citas y los turnos se guardan como hora local del salón.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.zona-horaria}") ZoneId zonaHoraria) {
        return Clock.system(zonaHoraria);
    }
}
