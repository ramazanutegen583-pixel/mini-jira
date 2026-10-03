package com.teamflow.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.teamflow.common.ApiException;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    public UserController(UserRepository users, PasswordEncoder passwords) { this.users = users; this.passwords = passwords; }
    public record UserView(UUID id, String name, String email, Role role) {
        public static UserView of(AppUser u) { return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole()); }
    }
    public record CreateUser(@NotBlank @Size(max=80) String name,
                             @NotBlank @Email @Size(max=160) String email,
                             @NotBlank @Size(min=12, max=64) String password,
                             @NotNull Role role) {}
    @GetMapping
    public List<UserView> list() { return users.findAll().stream().sorted(Comparator.comparing(AppUser::getName)).map(UserView::of).toList(); }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserView create(@Valid @RequestBody CreateUser request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT, "Этот email уже используется");
        return UserView.of(users.save(new AppUser(request.name().trim(), email, passwords.encode(request.password()), request.role())));
    }
}
