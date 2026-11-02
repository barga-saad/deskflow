package com.deskflow.auth;

import com.deskflow.auth.AuthDtos.AuthResponse;
import com.deskflow.auth.AuthDtos.LoginRequest;
import com.deskflow.auth.AuthDtos.RegisterRequest;
import com.deskflow.common.NotFoundException;
import com.deskflow.security.AuthenticatedUser;
import com.deskflow.user.UserRepository;
import com.deskflow.user.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return users.findById(currentUser.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
