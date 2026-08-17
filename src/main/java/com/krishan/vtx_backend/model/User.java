package com.krishan.vtx_backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;
    private int score = 0;
    private int rank = 0;
    private int problems = 0;
    private int streak = 0;
    private String githubUsername;
    private String leetcodeUsername;
    private String codeforcesUsername;
    private String hackerrankUsername;

    // Per-platform score breakdown (last sync pe bhara jaata hai) — chart ke liye
    private int githubScore = 0;
    private int leetcodeScore = 0;
    private int codeforcesScore = 0;
    private int hackerrankScore = 0;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    public int getProblems() { return problems; }
    public void setProblems(int problems) { this.problems = problems; }
    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }
    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String g) { this.githubUsername = g; }
    public String getLeetcodeUsername() { return leetcodeUsername; }
    public void setLeetcodeUsername(String l) { this.leetcodeUsername = l; }
    public String getCodeforcesUsername() { return codeforcesUsername; }
    public void setCodeforcesUsername(String c) { this.codeforcesUsername = c; }
    public String getHackerrankUsername() { return hackerrankUsername; }
    public void setHackerrankUsername(String h) { this.hackerrankUsername = h; }
    public int getGithubScore() { return githubScore; }
    public void setGithubScore(int s) { this.githubScore = s; }
    public int getLeetcodeScore() { return leetcodeScore; }
    public void setLeetcodeScore(int s) { this.leetcodeScore = s; }
    public int getCodeforcesScore() { return codeforcesScore; }
    public void setCodeforcesScore(int s) { this.codeforcesScore = s; }
    public int getHackerrankScore() { return hackerrankScore; }
    public void setHackerrankScore(int s) { this.hackerrankScore = s; }
}