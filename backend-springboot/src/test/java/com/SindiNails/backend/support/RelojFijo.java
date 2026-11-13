package com.SindiNails.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Reloj congelado para los tests: "ahora" es siempre {@link #AHORA}.
 * Uso: {@code @Import(RelojFijo.class)}. Sustituye al reloj real (ClockConfig) gracias a {@code @Primary}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RelojFijo {

    /** Martes 15 de enero de 2030, 10:00. */
    public static final LocalDateTime AHORA = LocalDateTime.of(2030, 1, 15, 10, 0);

    @Bean
    @Primary
    Clock relojFijo() {
        ZoneId zona = ZoneId.systemDefault();
        return Clock.fixed(AHORA.atZone(zona).toInstant(), zona);
    }
}
