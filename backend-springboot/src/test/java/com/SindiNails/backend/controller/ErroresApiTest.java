package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.EstadoCita;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.SindiNails.backend.support.DatosDePrueba;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los errores que el servicio explica con un mensaje llegan al cliente con su estado HTTP
 * y el cuerpo {"message": "..."}, que el frontend muestra tal cual:
 * una regla del salón incumplida → 409; datos de la petición no válidos → 400.
 *
 * Servidor real (RANDOM_PORT): el estado que recibe el cliente depende también de la redirección a /error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DatosDePrueba.class})
class ErroresApiTest {

    @Autowired TestRestTemplate http;
    @Autowired DatosDePrueba datos;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private Usuario usuario(Role role) {
        return usuarios.save(Usuario.builder().nombre("Ana").apellidos("Pérez")
                .email("ana-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password(passwordEncoder.encode("secreta1")).role(role).build());
    }

    private ResponseEntity<String> peticion(HttpMethod metodo, String url, Usuario usuario, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) h.setBearerAuth(jwtService.generateToken(usuario));
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private String mensaje(ResponseEntity<String> r) throws Exception {
        return json.readTree(r.getBody()).path("message").asText(null);
    }

    @Test
    void registrarse_con_un_email_en_uso_responde_409_con_el_mensaje() throws Exception {
        Usuario existente = usuario(Role.USER);

        var r = peticion(HttpMethod.POST, "/api/auth/register", null, """
                {"nombre":"Otra","apellidos":"Pérez","email":"%s","telefono":"600","password":"otra"}
                """.formatted(existente.getEmail()));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("El email ya está en uso.");
    }

    @Test
    void cancelar_una_cita_ya_cancelada_responde_409_con_el_mensaje() throws Exception {
        Usuario clienta = usuario(Role.USER);
        var cita = datos.cita(clienta, datos.trabajadora("Sindi"), datos.servicio("Manicura", 60),
                LocalDateTime.now().plusDays(3), EstadoCita.CANCELADA);

        var r = peticion(HttpMethod.PATCH, "/api/citas/" + cita.getId() + "/cancelar", clienta, null);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mensaje(r)).isEqualTo("La cita ya está cancelada.");
    }

    @Test
    void un_bloqueo_de_trabajadora_sin_trabajadora_responde_400_con_el_mensaje() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/admin/bloqueos", usuario(Role.ADMIN), """
                {"fechaHoraInicio":"2030-01-10T10:00:00","fechaHoraFin":"2030-01-10T12:00:00",
                 "motivo":"Médico","tipo":"TRABAJADOR"}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("El ID del trabajador es obligatorio para bloqueos de tipo TRABAJADOR.");
    }

    @Test
    void un_campo_que_no_pasa_la_validacion_responde_400_con_su_mensaje() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/admin/servicios", usuario(Role.ADMIN), """
                {"nombre":"Manicura","precio":20,"duracionEnMinutos":0,"tipoServicioId":1}
                """);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("La duración debe ser de al menos 1 minuto.");
    }

    @Test
    void con_varios_campos_no_validos_el_mensaje_los_reune_ordenados_por_campo() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/admin/servicios", usuario(Role.ADMIN), "{}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("La duración no puede ser nula. El nombre no puede estar vacío. "
                + "El precio no puede ser nulo. El ID del tipo de servicio no puede ser nulo.");
    }
}
