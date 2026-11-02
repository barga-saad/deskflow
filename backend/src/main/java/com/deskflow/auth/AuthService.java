package com.deskflow.auth;

import com.deskflow.auth.AuthDtos.AuthResponse;
import com.deskflow.auth.AuthDtos.LoginRequest;
import com.deskflow.auth.AuthDtos.RegisterRequest;
import com.deskflow.common.ConflictException;
import com.deskflow.security.JwtService;
import com.deskflow.user.Role;
import com.deskflow.user.User;
import com.deskflow.user.UserRepository;
import com.deskflow.user.UserResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("This email is already registered");
        }
        // Self-registration always creates a customer. Only an admin can grant staff roles.
        User user = users.save(new User(email, request.fullName().trim(),
                passwordEncoder.encode(request.password()), Role.CUSTOMER));
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.issue(user), UserResponse.from(user));
    }
}
