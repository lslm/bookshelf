package br.umc.demo.service;

import br.umc.demo.dto.AuthResponse;
import br.umc.demo.dto.LoginRequest;
import br.umc.demo.dto.RegisterRequest;
import br.umc.demo.entity.Role;
import br.umc.demo.entity.User;
import br.umc.demo.repository.UserRepository;
import br.umc.demo.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User usuarioSalvo;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        usuarioSalvo = new User();
        usuarioSalvo.setId(1L);
        usuarioSalvo.setFullName("Ana Lima");
        usuarioSalvo.setEmail("ana@email.com");
        usuarioSalvo.setPhone("11999990000");
        usuarioSalvo.setPassword("hash-argon2");
        usuarioSalvo.setRole(Role.USER);

        userDetails = org.springframework.security.core.userdetails.User
                .withUsername("ana@email.com")
                .password("hash-argon2")
                .authorities(List.of())
                .build();
    }

    // -------------------------------------------------------
    // register
    // -------------------------------------------------------

    @Test
    void register_comEmailNovo_deveSalvarUsuarioERetornarToken() {
        // setup
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash-argon2");
        when(userRepository.save(any(User.class))).thenReturn(usuarioSalvo);
        when(userDetailsService.loadUserByUsername("ana@email.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-gerado");

        // exercise
        AuthResponse resultado = authService.register(buildRegister("ana@email.com", "Senha@123"));

        // verify
        assertThat(resultado.getToken()).isEqualTo("jwt-token-gerado");
        assertThat(resultado.getEmail()).isEqualTo("ana@email.com");
        assertThat(resultado.getRole()).isEqualTo("USER");
    }

    @Test
    void register_deveSempreHashearASenha() {
        // setup
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("hash-argon2");
        when(userRepository.save(any(User.class))).thenReturn(usuarioSalvo);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        // exercise
        authService.register(buildRegister("ana@email.com", "Senha@123"));

        // verify
        verify(passwordEncoder).encode("Senha@123");
    }

    @Test
    void register_deveSempreAtribuirRoleUSER() {
        // setup
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(usuarioSalvo);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(userDetails);
        when(jwtService.generateToken(any())).thenReturn("token");

        // exercise
        authService.register(buildRegister("novo@email.com", "Senha@123"));

        // verify — inspeciona o objeto passado para save() sem poluir o thenAnswer com assertivas
        verify(userRepository).save(argThat(u -> Role.USER.equals(u.getRole())));
    }

    @Test
    void register_comEmailDuplicado_deveLancar409() {
        // setup
        when(userRepository.existsByEmail("ana@email.com")).thenReturn(true);

        // exercise + verify
        assertThatThrownBy(() -> authService.register(buildRegister("ana@email.com", "Senha@123")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");

        // verify
        verify(userRepository, never()).save(any());
    }

    // -------------------------------------------------------
    // login
    // -------------------------------------------------------

    @Test
    void login_comCredenciaisValidas_deveRetornarToken() {
        // setup
        when(userDetailsService.loadUserByUsername("ana@email.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token-login");
        when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(usuarioSalvo));

        // exercise
        AuthResponse resultado = authService.login(buildLogin("ana@email.com", "Senha@123"));

        // verify
        assertThat(resultado.getToken()).isEqualTo("jwt-token-login");
        assertThat(resultado.getEmail()).isEqualTo("ana@email.com");
    }

    @Test
    void login_comCredenciaisInvalidas_deveLancar401() {
        // setup
        doThrow(new BadCredentialsException("inválido"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // exercise + verify
        assertThatThrownBy(() -> authService.login(buildLogin("ana@email.com", "senhaErrada")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    void login_comCredenciaisInvalidas_naoDeveGerarToken() {
        // setup
        doThrow(new BadCredentialsException("inválido"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // exercise + verify
        assertThatThrownBy(() -> authService.login(buildLogin("ana@email.com", "senhaErrada")))
                .isInstanceOf(ResponseStatusException.class);

        // verify
        verify(jwtService, never()).generateToken(any());
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private RegisterRequest buildRegister(String email, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setFullName("Ana Lima");
        r.setEmail(email);
        r.setPhone("11999990000");
        r.setPassword(password);
        return r;
    }

    private LoginRequest buildLogin(String email, String password) {
        LoginRequest l = new LoginRequest();
        l.setEmail(email);
        l.setPassword(password);
        return l;
    }
}
