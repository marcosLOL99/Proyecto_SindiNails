package com.SindiNails.backend.controller;

import com.SindiNails.backend.TestcontainersConfiguration;
import com.SindiNails.backend.model.Role;
import com.SindiNails.backend.model.Usuario;
import com.SindiNails.backend.repository.RepositorioUsuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
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

import java.security.Key;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CHARACTERISATION TESTS -- documentan el comportamiento ACTUAL de la autenticación por HTTP, NO lo que debería hacer.
 *
 * Fijan qué responde hoy la API (estado y cuerpo) en registro, login y /api/users/me, con y sin token,
 * para poder cambiar el manejo de errores y la seguridad (Fase 2) sabiendo qué cambia.
 * Los casos SUSPICIOUS parecen bugs: se documentan tal cual y se corrigen con TDD como cambio aparte.
 *
 * Usan un servidor real (RANDOM_PORT) y no MockMvc: MockMvc no hace la redirección interna a /error
 * de Tomcat, y esa redirección decide qué estado recibe de verdad el frontend.
 * Los 401 (sin sesión válida, credenciales incorrectas) ya tienen tests permanentes en AutenticacionTest.
 * No son @Transactional (el servidor corre en otro hilo): cada test usa un email único.
 *
 * Son temporales: se sustituirán por tests de comportamiento al corregir la API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthCharacterisationTest {

    @Autowired TestRestTemplate http;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ObjectMapper json;

    @Value("${app.security.jwt.secret-key}") String secreto;
    @Value("${app.security.jwt.expiration}") long duracionToken;

    // ---------- utilidades ----------

    private static String emailUnico() {
        return "ana-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private Usuario usuarioGuardado(String email, String password, Role role) {
        return usuarios.save(Usuario.builder().nombre("Ana").apellidos("Pérez").email(email)
                .telefono("600000000").password(passwordEncoder.encode(password)).role(role).build());
    }

    private ResponseEntity<String> post(String url, String cuerpo) {
        return conToken(HttpMethod.POST, url, null, cuerpo);
    }

    private ResponseEntity<String> conToken(HttpMethod metodo, String url, String token, String cuerpo) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        return http.exchange(url, metodo, new HttpEntity<>(cuerpo, h), String.class);
    }

    private String token(String email, long msHastaCaducar, String secretoBase64) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        return Jwts.builder().setSubject(email).claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + msHastaCaducar))
                .signWith(key, SignatureAlgorithm.HS256).compact();
    }

    private Claims claimsDelToken(ResponseEntity<String> r) throws Exception {
        String token = json.readTree(r.getBody()).get("token").asText();
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    /** "estado cuerpo", para fijar ambas cosas a la vez. */
    private static String resumen(ResponseEntity<String> r) {
        return r.getStatusCode().value() + " " + r.getBody();
    }

    // ---------- registro ----------

    @Nested
    class Registro {

        @Test
        void characterises_registro_valido() throws Exception {
            String email = emailUnico();
            var r = post("/api/auth/register", """
                    {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000","password":"secreta1"}
                    """.formatted(email));

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(json.readTree(r.getBody()).fieldNames()).toIterable().containsExactly("token");
            Claims c = claimsDelToken(r);
            assertThat(c.getSubject()).isEqualTo(email);
            assertThat(c.get("role")).isEqualTo("USER");
            Usuario guardado = usuarios.findByEmail(email).orElseThrow();
            assertThat(guardado.getRole()).isEqualTo(Role.USER);
            assertThat(guardado.getNombre()).isEqualTo("Ana");
            assertThat(guardado.getApellidos()).isEqualTo("Pérez");
            assertThat(guardado.getTelefono()).isEqualTo("600000000");
            assertThat(passwordEncoder.matches("secreta1", guardado.getPassword())).isTrue();
        }

        @Test
        void characterises_registro_que_intenta_ser_admin_queda_como_USER() {
            String email = emailUnico();
            post("/api/auth/register", """
                    {"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600","password":"x","role":"ADMIN"}
                    """.formatted(email));

            assertThat(usuarios.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.USER);
        }

        @Test
        void characterises_registro_con_email_repetido__SUSPICIOUS_500_sin_mensaje() throws Exception {
            // SUSPICIOUS: el servicio lanza "El email ya está en uso." pero llega 500 sin el mensaje.
            String email = emailUnico();
            usuarioGuardado(email, "secreta1", Role.USER);
            var r = post("/api/auth/register", """
                    {"nombre":"Otra","apellidos":"Pérez","email":"%s","telefono":"600","password":"otra"}
                    """.formatted(email));

            assertThat(r.getStatusCode().value()).isEqualTo(500);
            assertThat(json.readTree(r.getBody()).has("message")).isFalse();
        }

        @Test
        void characterises_registro_sin_datos__SUSPICIOUS_500() throws Exception {
            // SUSPICIOUS: sin validación; falla al guardar (email NOT NULL) y llega 500 sin mensaje.
            var r = post("/api/auth/register", "{}");

            assertThat(r.getStatusCode().value()).isEqualTo(500);
            assertThat(json.readTree(r.getBody()).has("message")).isFalse();
        }

        @Test
        void characterises_registro_con_datos_invalidos__SUSPICIOUS_se_acepta() throws Exception {
            // SUSPICIOUS: sin validación; registra un email que no es un email, con nombre y contraseña vacíos.
            String email = "no-es-un-email-" + UUID.randomUUID().toString().substring(0, 8);
            var r = post("/api/auth/register", """
                    {"nombre":"","apellidos":"","email":"%s","telefono":"","password":""}
                    """.formatted(email));

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(claimsDelToken(r).getSubject()).isEqualTo(email);
            Usuario guardado = usuarios.findByEmail(email).orElseThrow();
            assertThat(guardado.getNombre()).isEmpty();
            assertThat(passwordEncoder.matches("", guardado.getPassword())).isTrue();
        }
    }

    // ---------- login ----------

    @Nested
    class Login {

        @Test
        void characterises_login_correcto() throws Exception {
            String email = emailUnico();
            usuarioGuardado(email, "secreta1", Role.ADMIN);
            var r = post("/api/auth/login", """
                    {"email":"%s","password":"secreta1"}
                    """.formatted(email));

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(json.readTree(r.getBody()).fieldNames()).toIterable().containsExactly("token");
            Claims c = claimsDelToken(r);
            assertThat(c.getSubject()).isEqualTo(email);
            assertThat(c.get("role")).isEqualTo("ADMIN");
            assertThat(c.getExpiration().getTime() - c.getIssuedAt().getTime()).isEqualTo(duracionToken);
        }



        @Test
        void characterises_login_sin_datos__SUSPICIOUS_401() {
            // SUSPICIOUS: sin validación; un login vacío se trata como credenciales incorrectas (no 400).
            var r = post("/api/auth/login", "{}");

            assertThat(resumen(r)).isEqualTo("401 null");
        }
    }

    // ---------- /api/users/me ----------

    @Nested
    class UsuarioActual {

        @Test
        void characterises_me_con_token_valido() throws Exception {
            String email = emailUnico();
            Usuario u = usuarioGuardado(email, "secreta1", Role.USER);
            var r = conToken(HttpMethod.GET, "/api/users/me", token(email, 60_000, secreto), null);

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(json.readTree(r.getBody())).isEqualTo(json.readTree("""
                    {"id":%d,"nombre":"Ana","apellidos":"Pérez","email":"%s","telefono":"600000000","role":"USER"}
                    """.formatted(u.getId(), email)));
        }






        @Test
        void characterises_editar_mis_datos_ignora_email_y_rol() throws Exception {
            String email = emailUnico();
            Usuario u = usuarioGuardado(email, "secreta1", Role.USER);
            var r = conToken(HttpMethod.PUT, "/api/users/me", token(email, 60_000, secreto), """
                    {"nombre":"Ana María","apellidos":"López","telefono":"611111111","email":"otro@test.com","role":"ADMIN"}
                    """);

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            assertThat(json.readTree(r.getBody())).isEqualTo(json.readTree("""
                    {"id":%d,"nombre":"Ana María","apellidos":"López","email":"%s","telefono":"611111111","role":"USER"}
                    """.formatted(u.getId(), email)));
            Usuario guardado = usuarios.findByEmail(email).orElseThrow();
            assertThat(guardado.getNombre()).isEqualTo("Ana María");
            assertThat(guardado.getRole()).isEqualTo(Role.USER);
        }

        @Test
        void characterises_editar_mis_datos_vacios__SUSPICIOUS_borra_los_datos() throws Exception {
            // SUSPICIOUS: sin validación; un cuerpo vacío deja nombre, apellidos y teléfono a null.
            String email = emailUnico();
            Usuario u = usuarioGuardado(email, "secreta1", Role.USER);
            var r = conToken(HttpMethod.PUT, "/api/users/me", token(email, 60_000, secreto), "{}");

            assertThat(r.getStatusCode().value()).isEqualTo(200);
            JsonNode cuerpo = json.readTree(r.getBody());
            assertThat(cuerpo).isEqualTo(json.readTree("""
                    {"id":%d,"nombre":null,"apellidos":null,"email":"%s","telefono":null,"role":"USER"}
                    """.formatted(u.getId(), email)));
            assertThat(usuarios.findByEmail(email).orElseThrow().getNombre()).isNull();
        }
    }
}
