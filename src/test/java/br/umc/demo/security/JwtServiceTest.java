package br.umc.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // Mínimo de 256 bits (32 bytes) para HS256
    private static final String SECRET = "chave-secreta-de-teste-com-256-bits-minimo-ok!!";
    private static final long EXPIRATION_MS = 60_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION_MS);
    }

    @Test
    void generateToken_deveRetornarTokenNaoVazio() {
        // setup
        UserDetails usuario = userDetails("teste@email.com");

        // exercise
        String token = jwtService.generateToken(usuario);

        // verify
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_deveRetornarEmailDoSubject() {
        // setup
        String email = "fulano@email.com";
        String token = jwtService.generateToken(userDetails(email));

        // exercise
        String emailExtraido = jwtService.extractEmail(token);

        // verify
        assertThat(emailExtraido).isEqualTo(email);
    }

    @Test
    void isTokenValid_comTokenValidoDoMesmoUsuario_deveRetornarTrue() {
        // setup
        UserDetails usuario = userDetails("valido@email.com");
        String token = jwtService.generateToken(usuario);

        // exercise
        boolean valido = jwtService.isTokenValid(token, usuario);

        // verify
        assertThat(valido).isTrue();
    }

    @Test
    void isTokenValid_comTokenDeOutroUsuario_deveRetornarFalse() {
        // setup
        String token = jwtService.generateToken(userDetails("a@email.com"));
        UserDetails outroUsuario = userDetails("b@email.com");

        // exercise
        boolean valido = jwtService.isTokenValid(token, outroUsuario);

        // verify
        assertThat(valido).isFalse();
    }

    @Test
    void isTokenValid_comTokenExpirado_deveLancarExcecao() {
        // setup — gera token já expirado (expiration negativo) e restaura o valor original
        ReflectionTestUtils.setField(jwtService, "expiration", -1L);
        String token = jwtService.generateToken(userDetails("expirado@email.com"));
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION_MS);

        // exercise + verify — JJWT lança ExpiredJwtException ao parsear; o filtro captura essa exceção
        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails("expirado@email.com")))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractEmail_comTokenInvalido_deveLancarExcecao() {
        // setup
        String tokenInvalido = "token.invalido.qualquer";

        // exercise + verify
        assertThatThrownBy(() -> jwtService.extractEmail(tokenInvalido))
                .isInstanceOf(Exception.class);
    }

    private UserDetails userDetails(String email) {
        return User.withUsername(email).password("senha").authorities(List.of()).build();
    }
}
