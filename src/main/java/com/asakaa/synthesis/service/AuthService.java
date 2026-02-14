package com.asakaa.synthesis.service;

import com.asakaa.synthesis.domain.dto.request.AuthRequest;
import com.asakaa.synthesis.domain.dto.request.RegisterRequest;
import com.asakaa.synthesis.domain.dto.response.AuthResponse;
import com.asakaa.synthesis.domain.entity.Provider;
import com.asakaa.synthesis.exception.ValidationException;
import com.asakaa.synthesis.repository.ProviderRepository;
import com.asakaa.synthesis.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (providerRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered");
        }

        Provider provider = Provider.builder()
                .name(request.getName())
                .role(request.getRole())
                .clinicName(request.getClinicName())
                .region(request.getRegion())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        provider = providerRepository.save(provider);

        String token = jwtUtil.generateToken(provider.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(provider.getEmail())
                .name(provider.getName())
                .role(provider.getRole())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        Provider provider = providerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), provider.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(provider.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(provider.getEmail())
                .name(provider.getName())
                .role(provider.getRole())
                .build();
    }
}
