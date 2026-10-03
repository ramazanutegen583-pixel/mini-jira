package com.teamflow.user;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id private UUID id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, unique = true, length = 160) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Role role;
    protected AppUser() {}
    public AppUser(String name, String email, String passwordHash, Role role) {
        this.id = UUID.randomUUID(); this.name = name; this.email = email;
        this.passwordHash = passwordHash; this.role = role;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
