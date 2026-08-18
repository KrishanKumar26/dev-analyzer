package com.krishan.vtx_backend.model;

import jakarta.persistence.*;

// Ek din ka score snapshot — growth-over-time chart ke liye (per user, per date)
@Entity
@Table(name = "score_snapshots")
public class ScoreSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String snapDate;  // yyyy-MM-dd
    private int score;
    private int problems;
    private int rank;
    private int streak;

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String e) { this.userEmail = e; }
    public String getSnapDate() { return snapDate; }
    public void setSnapDate(String d) { this.snapDate = d; }
    public int getScore() { return score; }
    public void setScore(int s) { this.score = s; }
    public int getProblems() { return problems; }
    public void setProblems(int p) { this.problems = p; }
    public int getRank() { return rank; }
    public void setRank(int r) { this.rank = r; }
    public int getStreak() { return streak; }
    public void setStreak(int s) { this.streak = s; }
}
