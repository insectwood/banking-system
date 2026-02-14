package com.example.authservice.service;

import com.example.authservice.domain.User;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.SignupRequest;
import com.example.authservice.global.security.JwtProvider;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * Login processing
     * @return Issued JWT token and user information
     */
    public LoginResponse login(LoginRequest request) {
        // 1. Check for user existence
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email or Password Invalid."));

        // 2. Verify password match
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Email or Password Invalid.");
        }

        // 3. Generate JWT token (Using userUuid)
        String token = jwtProvider.createToken(user.getUserUuid(), user.getEmail());

        return new LoginResponse(token, user.getName(), user.getUserUuid());
    }

    @Transactional
    public void signup(SignupRequest request) {
        // 1. Check for duplicate email
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("This email is already in use.");
        }

        // 2. Encrypt password and create user
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // BCrypt encryption
                .name(request.name())
                .build();

        userRepository.save(user);
    }
}
