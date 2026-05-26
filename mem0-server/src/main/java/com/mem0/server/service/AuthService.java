package com.mem0.server.service;

import com.mem0.server.domain.model.User;
import com.mem0.server.repository.UserRepository;
import com.mem0.server.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for authentication operations.
 *
 * @author MoBai

 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user.
     *
     * @param email the email
     * @param password the password
     * @param name the name
     * @return the created user
     */
    @Transactional
    public User register(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email already exists: " + email);
        }

        String passwordHash = passwordEncoder.encode(password);
        User user = new User(email, passwordHash, name);
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        userRepository.save(user);

        log.info("Registered new user: {}", email);
        return user;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param email the email
     * @param password the password
     * @return the JWT token
     */
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

//        if (!user.isActive()) {
//            throw new IllegalArgumentException("User account is deactivated");
//        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        log.debug("User logged in: {}", email);
        return token;
    }

    /**
     * Refreshes a JWT token.
     *
     * @param refreshToken the refresh token
     * @return the new JWT token
     */
    public String refreshToken(String refreshToken) {
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String userId = jwtService.getUserId(refreshToken);
        String email = jwtService.getEmail(refreshToken);

        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
//
//        if (!user.isActive()) {
//            throw new IllegalArgumentException("User account is deactivated");
//        }

        return jwtService.generateToken(userId, email);
    }

    /**
     * Generates a refresh token for a user.
     *
     * @param email the email
     * @return the refresh token
     */
    public String generateRefreshToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return jwtService.generateRefreshToken(user.getId(), user.getEmail());
    }

    /**
     * Finds a user by email.
     *
     * @param email the email
     * @return the user
     */
    public User findUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }
}
