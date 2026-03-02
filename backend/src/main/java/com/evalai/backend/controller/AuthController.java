package com.evalai.backend.controller;

import com.evalai.backend.dto.AuthRequest;
import com.evalai.backend.dto.AuthResponse;
import com.evalai.backend.dto.RegisterRequest;
import com.evalai.backend.model.User;
import com.evalai.backend.repository.UserRepository;
import com.evalai.backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /** Registers a new user with BCrypt-hashed password. */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("{\"error\": \"Email already registered.\"}");
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, user.getName(), user.getEmail()));
    }

    /** Authenticates a user and returns a signed JWT token. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(user -> passwordEncoder.matches(request.password(), user.getPassword()))
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail());
                    return ResponseEntity.ok((Object) new AuthResponse(token, user.getName(), user.getEmail()));
                })
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\": \"Invalid email or password.\"}"));
    }
}
