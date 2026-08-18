package com.krishan.vtx_backend.model;

import jakarta.persistence.*;

// Ek coding goal (e.g. "Solve 100 LeetCode") — target vs current progress
@Entity
@Table(name = "goals")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String title;
    private int target;
    private int current;

    public Long getId() { return id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String e) { this.userEmail = e; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public int getTarget() { return target; }
    public void setTarget(int t) { this.target = t; }
    public int getCurrent() { return current; }
    public void setCurrent(int c) { this.current = c; }
}
