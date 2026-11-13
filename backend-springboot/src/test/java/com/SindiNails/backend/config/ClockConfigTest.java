package com.SindiNails.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "Ahora" para la aplicación es la hora del salón (app.zona-horaria), no la zona del sistema
 * donde se ejecute la JVM (en docker, UTC). Solo carga ClockConfig: no necesita base de datos.
 */
class ClockConfigTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(ClockConfig.class);

    @Test
    void el_reloj_usa_la_zona_horaria_configurada_y_no_la_del_sistema() {
        // Una zona que no es la de ningún equipo de desarrollo, para que el test no pase por casualidad.
        contexto.withPropertyValues("app.zona-horaria=Pacific/Auckland")
                .run(ctx -> assertThat(ctx.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Pacific/Auckland")));
    }
}
