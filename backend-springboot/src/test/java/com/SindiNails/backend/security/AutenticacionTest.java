package com.SindiNails.backend.security;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.SindiNails.backend.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin una sesión válida la API responde 401 (el frontend cierra la sesión y lleva al login);
 * con sesión válida pero sin permiso, 403; y un error de una petición con sesión válida
 * llega con su estado real, nunca como 401 (no debe echar a la clienta).
 *
 * Servidor real (RANDOM_PORT): el estado que recibe el cliente depende también de la redirección a /error.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AutenticacionTest {

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Value("${app.security.jwt.secret-key}") String secreto;

    private Usuario usuario(Role role) {
        return usuarios.save(Usuario.builder().nombre("Ana").apellidos("Pérez")
                .email("ana-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
                .telefono("600000000").password(passwordEncoder.encode("secreta1")).role(role).build());
    }

    private String tokenFirmado(String email, long msHastaCaducar, String secretoBase64) {
        return Jwts.builder().setSubject(email).claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + msHastaCaducar))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64)), SignatureAlgorithm.HS256)
                .compact();
    }

    private HttpStatusCode get(String url, String token) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.setBearerAuth(token);
        return http.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class).getStatusCode();
    }

    private HttpStatusCode login(String email, String password) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        String cuerpo = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
        return http.exchange("/api/auth/login", HttpMethod.POST, new HttpEntity<>(cuerpo, h), String.class)
                .getStatusCode();
    }

    @Nested
    class SinSesionValida_401 {

        @Test
        void sin_token() {
            assertThat(get("/api/users/me", null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void sin_token_en_la_administracion() {
            assertThat(get("/api/admin/citas", null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void con_token_caducado() {
            Usuario ana = usuario(Role.USER);
            assertThat(get("/api/users/me", tokenFirmado(ana.getEmail(), -60_000, secreto)))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void con_token_firmado_con_otro_secreto() {
            Usuario ana = usuario(Role.USER);
            String otroSecreto = "b3Ryby1zZWNyZXRvLWRpc3RpbnRvLXBhcmEtdGVzdHMh";
            assertThat(get("/api/users/me", tokenFirmado(ana.getEmail(), 60_000, otroSecreto)))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void con_token_mal_formado() {
            assertThat(get("/api/users/me", "esto-no-es-un-jwt")).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void con_token_de_un_usuario_que_ya_no_existe() {
            String emailBorrado = "borrada-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
            assertThat(get("/api/users/me", tokenFirmado(emailBorrado, 60_000, secreto)))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void login_con_password_incorrecta() {
            Usuario ana = usuario(Role.USER);
            assertThat(login(ana.getEmail(), "mala")).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void login_con_email_que_no_existe() {
            assertThat(login("nadie-" + UUID.randomUUID() + "@test.com", "secreta1"))
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class ConSesionValida {

        @Test
        void una_clienta_en_la_administracion_recibe_403() {
            String token = jwtService.generateToken(usuario(Role.USER));
            assertThat(get("/api/admin/citas", token)).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void un_token_invalido_no_impide_ver_el_catalogo_publico() {
            assertThat(get("/api/servicios", "esto-no-es-un-jwt")).isEqualTo(HttpStatus.OK);
        }

        @Test
        void un_error_llega_con_su_estado_real_y_no_como_401() {
            // ResourceNotFoundException (@ResponseStatus 404): la cita no existe.
            String token = jwtService.generateToken(usuario(Role.USER));
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            var r = http.exchange("/api/citas/999999/cancelar", HttpMethod.PATCH, new HttpEntity<>(h), String.class);
            assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
