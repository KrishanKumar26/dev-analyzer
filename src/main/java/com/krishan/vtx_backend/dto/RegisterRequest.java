package com.krishan.vtx_backend.dto;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String githubUsername;
    private String leetcodeUsername;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String g) { this.githubUsername = g; }
    public String getLeetcodeUsername() { return leetcodeUsername; }
    public void setLeetcodeUsername(String l) { this.leetcodeUsername = l; }
}