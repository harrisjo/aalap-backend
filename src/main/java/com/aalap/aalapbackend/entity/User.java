package com.aalap.aalapbackend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String bio;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Soft-delete flag. When true the account PII has been anonymized and the
     * user can no longer log in or authenticate via JWT.
     * Audio contributions and threads are intentionally kept so collaborators'
     * work is not broken.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // RBAC foundation: all authenticated users carry ROLE_USER.
        // When admin functionality is needed, add a `role` VARCHAR column to
        // the `users` table and read the persisted role here instead.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // A soft-deleted account has its PII anonymized but its audio contributions preserved.
        // Returning false here blocks login and JWT authentication automatically.
        return !deleted;
    }
}