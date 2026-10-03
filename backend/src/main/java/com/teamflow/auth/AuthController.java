package com.teamflow.auth;

import com.teamflow.common.ApiException;
import com.teamflow.user.*;
import com.teamflow.user.UserController.UserView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;
    private final String dummyHash;
    public AuthController(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer, @Value("${app.jwt.ttl}") Duration ttl) {
        this.users=users; this.passwords=passwords; this.encoder=encoder; this.issuer=issuer; this.ttl=ttl;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public record Login(@NotBlank @Email @Size(max=160) String email, @NotBlank @Size(max=64) String password) {}
    public record Session(String accessToken, Instant expiresAt, UserView user) {}
    @PostMapping("/login")
    public Session login(@Valid @RequestBody Login request) {
        var user = users.findByEmail(request.email().trim().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(request.password(), user.map(AppUser::getPasswordHash).orElse(dummyHash));
        if (user.isEmpty() || !matches) throw new ApiException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        AppUser account = user.get(); Instant now = Instant.now(); Instant expires = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(account.getId().toString())
            .audience(List.of("teamflow-api")).issuedAt(now).expiresAt(expires)
            .id(UUID.randomUUID().toString()).claim("roles", List.of(account.getRole().name())).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Session(token, expires, UserView.of(account));
    }
    @GetMapping("/me")
    public UserView me(@AuthenticationPrincipal Jwt jwt) {
        return UserView.of(users.findById(UUID.fromString(jwt.getSubject()))
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Аккаунт не найден")));
    }
}
