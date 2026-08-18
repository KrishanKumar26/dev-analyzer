package com.krishan.vtx_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String token;
    private long expiresAt; // epoch millis

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    public String getToken() { return token; }
    public void setToken(String t) { this.token = t; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long x) { this.expiresAt = x; }
}
