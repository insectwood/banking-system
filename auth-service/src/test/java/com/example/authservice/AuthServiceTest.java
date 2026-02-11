package com.example.authservice;

import com.example.authservice.domain.User;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.LoginResponse;
import com.example.authservice.dto.SignupRequest;
import com.example.authservice.global.security.JwtProvider;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Verify that the password is encrypted and stored during registration.")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest("test@test.com", "password123", "test name");
        given(userRepository.existsByEmail(anyString())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encrypted_password");

        // when
        authService.signup(request);

        // then
        verify(userRepository).save(argThat(user->
                user.getEmail().equals("test@test.com") &&
                        user.getPassword().equals("encrypted_password") &&
                        user.getUserUuid() != null
        ));
    }

    @Test
    @DisplayName("Verify that a token is returned upon successful login.")
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        User user = User.builder()
                .email("test@test.com")
                .password("encrypted_password")
                .name("test name")
                .build();

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.createToken(anyString(), anyString())).willReturn("test_jwt_token");

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("test_jwt_token");
        assertThat(response.userUuid()).isEqualTo(user.getUserUuid());
    }

    @Test
    @DisplayName("Verify that an exception is thrown when the password does not match.")
    void login_Fail_PasswordMismatch() {
        // given
        LoginRequest request = new LoginRequest("test@test.com", "wrong_password");
        User user = User.builder()
                .email("test@test.com")
                .password("encrypted_password")
                .build();

        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid password.");
    }
}
