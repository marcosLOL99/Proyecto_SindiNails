package com.SindiNails.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MySQL 8 desechable para los tests de integración.
 * {@code @ServiceConnection} sustituye la URL, el usuario y la contraseña del datasource,
 * así los tests nunca tocan el MySQL de desarrollo.
 * Uso: {@code @Import(TestcontainersConfiguration.class)} en cada test de Spring.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));
    }
}
