package com.dfs.auth;

import com.dfs.auth.dto.AuthResponse;
import com.dfs.user.User;
import com.dfs.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

    @Test
    void registerHashesPasswordSavesUserAndReturnsToken() {
        when(userRepository.existsByEmail("naresh@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("naresh@example.com")).thenReturn("JWT_TOKEN");

        AuthResponse response = authService.register("Naresh@Example.com", "plaintext123");

        assertThat(response.token()).isEqualTo("JWT_TOKEN");
        assertThat(response.email()).isEqualTo("naresh@example.com");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("taken@example.com", "plaintext123"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void loginReturnsTokenWhenCredentialsValid() {
        User user = new User("naresh@example.com", "HASHED");
        when(userRepository.findByEmail("naresh@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plaintext123", "HASHED")).thenReturn(true);
        when(jwtService.generateToken("naresh@example.com")).thenReturn("JWT_TOKEN");

        AuthResponse response = authService.login("naresh@example.com", "plaintext123");

        assertThat(response.token()).isEqualTo("JWT_TOKEN");
        assertThat(response.email()).isEqualTo("naresh@example.com");
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("naresh@example.com", "HASHED");
        when(userRepository.findByEmail("naresh@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "HASHED")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("naresh@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
