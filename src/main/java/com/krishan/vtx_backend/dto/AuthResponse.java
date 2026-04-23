package com.krishan.vtx_backend.dto;

public class AuthResponse {
    private String token;
    private String name;
    private String email;
    private int score;
    private int rank;
    private int streak;

    public AuthResponse(String token, String name, String email,
                        int score, int rank, int streak) {
        this.token = token;
        this.name = name;
        this.email = email;
        this.score = score;
        this.rank = rank;
        this.streak = streak;
    }

    public String getToken() { return token; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getScore() { return score; }
    public int getRank() { return rank; }
    public int getStreak() { return streak; }
}