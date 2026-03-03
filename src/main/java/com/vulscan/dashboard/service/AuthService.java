package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.AuthTokenResponseDto;
import com.vulscan.dashboard.dto.LoginRequestDto;
import com.vulscan.dashboard.dto.RefreshTokenRequestDto;
import com.vulscan.dashboard.entity.AppUser;
import com.vulscan.dashboard.entity.UserStatus;
import com.vulscan.dashboard.repository.UserRepository;
import com.vulscan.dashboard.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager,
                       UserRepository userRepository,
                       JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthTokenResponseDto login(LoginRequestDto request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        AppUser user = userRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("User is not active");
        }

        return buildTokenResponse(user);
    }

    public AuthTokenResponseDto refresh(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        try {
            if (!jwtService.isTokenType(refreshToken, "refresh")) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            String email = jwtService.extractUsername(refreshToken);
            AppUser user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadCredentialsException("User is not active");
            }

            return buildTokenResponse(user);
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    private AuthTokenResponseDto buildTokenResponse(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthTokenResponseDto(accessToken, refreshToken, "Bearer", jwtService.getAccessExpiresInSeconds());
    }
}
