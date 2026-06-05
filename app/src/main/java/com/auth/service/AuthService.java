package com.auth.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.auth.dto.AuthResponse;
import com.auth.dto.LoginRequest;
import com.auth.dto.RegisterRequest;
import com.auth.dto.TokenRefreshRequest;
import com.auth.dto.UpdateProfileRequest;
import com.auth.model.RefreshToken;
import com.auth.model.User;
import com.auth.repository.RefreshTokenRepository;
import com.auth.repository.UserRepository;
import static com.auth.service.JwtService.ACCESS_TOKEN_EXPIRATION;
import static com.auth.service.JwtService.REFRESH_TOKEN_EXPIRATION;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        
        userRepository.save(user);
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (oldToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(oldToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        // Rotate: delete the old token before issuing a new one
        refreshTokenRepository.delete(oldToken);
        refreshTokenRepository.flush();

        String accessToken = jwtService.generateAccessToken(oldToken.getUser().getUsername());
        String newRefreshTokenStr = jwtService.generateRefreshToken(oldToken.getUser().getUsername());
        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(oldToken.getUser())
                .token(newRefreshTokenStr)
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return new AuthResponse(accessToken, newRefreshToken.getToken(), ACCESS_TOKEN_EXPIRATION);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    private AuthResponse generateAuthResponse(User user) {
        // Clear existing tokens to ensure only one active session per user (optional policy)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.flush();

        String accessToken = jwtService.generateAccessToken(user.getUsername());
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtService.generateRefreshToken(user.getUsername()))
                .expiryDate(Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION))
                .build();
        
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, refreshToken.getToken(), ACCESS_TOKEN_EXPIRATION);
    }

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public void updateProfile(User loggedInUser, UpdateProfileRequest request) {
        User dbUser = userRepository.findById(loggedInUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getUsername() != null && !request.getUsername().equals(dbUser.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already exists");
            }
            dbUser.setUsername(request.getUsername());
        }

        if (request.getProfileName() != null) {
            dbUser.setProfileName(request.getProfileName());
        }

        if (request.getBio() != null) {
            dbUser.setBio(request.getBio());
        }

        userRepository.save(dbUser);
    }

    @Transactional
    public void uploadAvatar(User loggedInUser, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        User dbUser = userRepository.findById(loggedInUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        dbUser.setProfilePictureBlob(convertToPng(file));
        userRepository.save(dbUser);
    }

    private byte[] convertToPng(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchUsers(query);
    }

    public Page<User> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable);
    }
}