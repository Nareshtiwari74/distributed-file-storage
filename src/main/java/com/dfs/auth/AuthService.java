package com.dfs.auth;

import com.dfs.auth.dto.AuthResponse;
import com.dfs.user.User;
import com.dfs.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        String hashed = passwordEncoder.encode(rawPassword);
        User saved = userRepository.save(new User(normalizedEmail, hashed));
        log.info("Registered new user id={} email={}", saved.getId(), saved.getEmail());

        String token = jwtService.generateToken(saved.getEmail());
        return AuthResponse.bearer(token, saved.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        log.info("User logged in id={} email={}", user.getId(), user.getEmail());
        String token = jwtService.generateToken(user.getEmail());
        return AuthResponse.bearer(token, user.getEmail());
    }
}
