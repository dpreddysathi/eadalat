package com.eadalat.auth.service;

import com.eadalat.auth.dto.AuthResponse;
import com.eadalat.auth.dto.LoginRequest;
import com.eadalat.auth.dto.RegisterRequest;
import com.eadalat.auth.dto.UserResponse;
import com.eadalat.auth.entity.Role;
import com.eadalat.auth.entity.User;
import com.eadalat.auth.exception.ConflictException;
import com.eadalat.auth.exception.ForbiddenException;
import com.eadalat.auth.exception.NotFoundException;
import com.eadalat.auth.exception.UnauthorizedException;
import com.eadalat.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Roles that anyone may self-register as. */
    private static final Set<Role> SELF_REGISTERABLE = Set.of(Role.PETITIONER, Role.LAWYER);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest req, boolean requesterIsAdmin) {
        Role role = req.role() != null ? req.role() : Role.PETITIONER;

        if (!SELF_REGISTERABLE.contains(role) && !requesterIsAdmin) {
            throw new ForbiddenException("Only an ADMIN can create " + role + " accounts");
        }

        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = User.builder()
                .name(req.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .build();
        User saved = users.save(user);
        log.info("Registered user id={} role={}", saved.getId(), saved.getRole());
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = users.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        log.info("User id={} logged in", user.getId());
        return new AuthResponse(token, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.from(user);
    }
}
