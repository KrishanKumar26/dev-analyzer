package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        HashMap<String, Object> response = new HashMap<>();
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("score", user.getScore());
        response.put("rank", user.getRank());
        response.put("problems", user.getProblems());
        response.put("streak", user.getStreak());
        response.put("githubUsername", user.getGithubUsername() != null ? user.getGithubUsername() : "");
        response.put("leetcodeUsername", user.getLeetcodeUsername() != null ? user.getLeetcodeUsername() : "");
        response.put("codeforcesUsername", user.getCodeforcesUsername() != null ? user.getCodeforcesUsername() : "");
        response.put("hackerrankUsername", user.getHackerrankUsername() != null ? user.getHackerrankUsername() : "");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updates) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("name") && !updates.get("name").isEmpty())
            user.setName(updates.get("name"));
        if (updates.containsKey("githubUsername"))
            user.setGithubUsername(updates.get("githubUsername"));
        if (updates.containsKey("leetcodeUsername"))
            user.setLeetcodeUsername(updates.get("leetcodeUsername"));
        if (updates.containsKey("codeforcesUsername"))
            user.setCodeforcesUsername(updates.get("codeforcesUsername"));
        if (updates.containsKey("hackerrankUsername"))
            user.setHackerrankUsername(updates.get("hackerrankUsername"));

        userRepository.save(user);

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Profile updated!");
        res.put("name", user.getName());
        res.put("githubUsername", user.getGithubUsername() != null ? user.getGithubUsername() : "");
        res.put("leetcodeUsername", user.getLeetcodeUsername() != null ? user.getLeetcodeUsername() : "");
        res.put("codeforcesUsername", user.getCodeforcesUsername() != null ? user.getCodeforcesUsername() : "");
        res.put("hackerrankUsername", user.getHackerrankUsername() != null ? user.getHackerrankUsername() : "");
        return ResponseEntity.ok(res);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        List<User> users = userRepository.findAll();
        users.sort((a, b) -> b.getScore() - a.getScore());

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        int limit = Math.min(users.size(), 10);
        for (int i = 0; i < limit; i++) {
            User u = users.get(i);
            HashMap<String, Object> m = new HashMap<>();
            m.put("name", u.getName());
            m.put("score", u.getScore());
            m.put("rank", i + 1);
            m.put("platform", u.getGithubUsername() != null && !u.getGithubUsername().isEmpty()
                    ? "GitHub" : "LeetCode");
            leaderboard.add(m);
        }
        return ResponseEntity.ok(leaderboard);
    }

    @PutMapping("/update-stats")
    public ResponseEntity<?> updateStats(@RequestBody Map<String, Integer> stats) {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (stats.containsKey("score")) user.setScore(stats.get("score"));
        if (stats.containsKey("problems")) user.setProblems(stats.get("problems"));
        if (stats.containsKey("streak")) user.setStreak(stats.get("streak"));

        // Auto rank update based on score
        List<User> allUsers = userRepository.findAll();
        allUsers.sort((a, b) -> b.getScore() - a.getScore());
        for (int i = 0; i < allUsers.size(); i++) {
            allUsers.get(i).setRank(i + 1);
            userRepository.save(allUsers.get(i));
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Stats updated!");
        res.put("score", user.getScore());
        res.put("rank", user.getRank());
        return ResponseEntity.ok(res);
    }
}