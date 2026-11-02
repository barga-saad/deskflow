package com.deskflow.user;

import com.deskflow.common.ConflictException;
import com.deskflow.common.NotFoundException;
import com.deskflow.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    public record RoleUpdateRequest(@NotNull Role role) {
    }

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        return users.findAll(Sort.by("fullName")).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<UserResponse> agents() {
        return users.findByRoleInOrderByFullName(List.of(Role.AGENT, Role.ADMIN)).stream()
                .map(UserResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateRole(@PathVariable Long id,
                                   @Valid @RequestBody RoleUpdateRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (id.equals(currentUser.id())) {
            throw new ConflictException("You cannot change your own role");
        }
        User user = users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        user.setRole(request.role());
        return UserResponse.from(user);
    }
}
