package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registro, login y edición del perfil rechazan los datos incompletos o no válidos con 400
 * y los mensajes de los campos (ordenados por campo), sin guardar nada.
 * Mismas reglas que el formulario de registro del frontend: todos los campos obligatorios y un email válido.
 *
 * Servidor real (RANDOM_PORT): el estado que recibe el cliente depende también de la redirección a /error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ValidacionAutenticacionTest {

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper json;

    private static String emailUnico() {
        return "ana-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
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
    void registrarse_sin_datos_responde_400_con_todos_los_campos_obligatorios() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/auth/register", null, "{}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("Los apellidos son obligatorios. El email es obligatorio. "
                + "El nombre es obligatorio. La contraseña es obligatoria. El teléfono es obligatorio.");
    }

    @Test
    void registrarse_con_un_email_no_valido_o_campos_en_blanco_no_crea_la_cuenta() throws Exception {
        String email = "no-es-un-email-" + UUID.randomUUID().toString().substring(0, 8);

        var r = peticion(HttpMethod.POST, "/api/auth/register", null, """
                {"nombre":" ","apellidos":"Pérez","email":"%s","telefono":"600","password":"secreta1"}
                """.formatted(email));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("El email no es válido. El nombre es obligatorio.");
        assertThat(usuarios.findByEmail(email)).isEmpty();
    }

    @Test
    void un_campo_que_incumple_dos_reglas_da_sus_mensajes_siempre_en_el_mismo_orden() throws Exception {
        // "   " incumple @NotBlank y @Email; el validador no garantiza en qué orden los informa.
        for (int i = 0; i < 20; i++) {
            var r = peticion(HttpMethod.POST, "/api/auth/register", null, """
                    {"nombre":"Ana","apellidos":"Pérez","email":"   ","telefono":"600","password":"secreta1"}
                    """);

            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(mensaje(r)).isEqualTo("El email es obligatorio. El email no es válido.");
        }
    }

    @Test
    void iniciar_sesion_sin_datos_responde_400_y_no_401() throws Exception {
        var r = peticion(HttpMethod.POST, "/api/auth/login", null, "{}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("El email es obligatorio. La contraseña es obligatoria.");
    }

    @Test
    void editar_mis_datos_vacios_responde_400_y_no_borra_nada() throws Exception {
        Usuario ana = usuarios.save(Usuario.builder().nombre("Ana").apellidos("Pérez").email(emailUnico())
                .telefono("600000000").password(passwordEncoder.encode("secreta1")).role(Role.USER).build());

        var r = peticion(HttpMethod.PUT, "/api/users/me", ana, "{}");

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mensaje(r)).isEqualTo("Los apellidos son obligatorios. El nombre es obligatorio. "
                + "El teléfono es obligatorio.");
        Usuario guardada = usuarios.findByEmail(ana.getEmail()).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Ana");
        assertThat(guardada.getApellidos()).isEqualTo("Pérez");
        assertThat(guardada.getTelefono()).isEqualTo("600000000");
    }
}
