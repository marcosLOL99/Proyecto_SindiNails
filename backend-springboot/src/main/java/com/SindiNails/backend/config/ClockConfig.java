package com.SindiNails.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj de la aplicación. Los servicios que dependen de "ahora" lo reciben inyectado
 * en vez de llamar a LocalDateTime.now(), así los tests pueden fijar la fecha y la hora.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
