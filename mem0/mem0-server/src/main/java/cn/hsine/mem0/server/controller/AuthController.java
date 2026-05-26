package cn.hsine.mem0.server.controller;

import cn.hsine.mem0.server.domain.model.User;
import cn.hsine.mem0.server.dto.request.LoginRequest;
import cn.hsine.mem0.server.dto.request.RefreshTokenRequest;
import cn.hsine.mem0.server.dto.request.RegisterRequest;
import cn.hsine.mem0.server.dto.response.AuthResponse;
import cn.hsine.mem0.server.dto.response.UserResponse;
import cn.hsine.mem0.server.security.JwtService;
import cn.hsine.mem0.server.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * @author MoBai
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        User user = authService.register(request.email(), request.password(), request.name());
        UserResponse response = new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
        return ResponseEntity.ok(response);
    }

    /**
     * Logs in a user and returns JWT tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        String token = authService.login(request.email(), request.password());
        String refreshToken = authService.generateRefreshToken(request.email());

        User user = authService.findUserByEmail(request.email());
        AuthResponse response = new AuthResponse(token, refreshToken, user.getId(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes a JWT token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        String newToken = authService.refreshToken(request.refreshToken());
        String userId = jwtService.getUserId(request.refreshToken());
        String email = jwtService.getEmail(request.refreshToken());

        AuthResponse response = new AuthResponse(newToken, null, userId, email);
        return ResponseEntity.ok(response);
    }
}
