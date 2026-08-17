package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    // Optional GitHub token: agar set hai to 5000 req/hour, warna sirf 60/hour (rate-limited)
    @Value("${github.token:}")
    private String githubToken;

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

    // GitHub Real Data API
    @GetMapping("/github/{username}")
    public ResponseEntity<?> getGithubUser(@PathVariable String username) {
        try {
            URL url = new URL("https://api.github.com/users/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "VortexApp");
            // Token ho to authenticate karo -> rate limit 60/hr se 5000/hr
            if (githubToken != null && !githubToken.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + githubToken);
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int status = conn.getResponseCode();
            if (status != 200) {
                HashMap<String, Object> err = new HashMap<>();
                if (status == 404) {
                    err.put("error", "GitHub user not found");
                } else if (status == 403 || status == 429) {
                    // Rate limit hit — isko "not found" mat dikhao, warna galat message jaata hai
                    err.put("error", "GitHub rate limit reached, thodi der baad try karo");
                } else {
                    err.put("error", "GitHub request failed (status " + status + ")");
                }
                return ResponseEntity.status(status == 404 ? 404 : 502).body(err);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            // Raw JSON string return karo
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(sb.toString());

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "GitHub fetch failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    // LeetCode Real Data API
    @GetMapping("/leetcode/{username}")
    public ResponseEntity<?> getLeetcodeUser(@PathVariable String username) {
        try {
            String query = "{\"query\":\"{ matchedUser(username: \\\"" + username + "\\\") { username submitStats: submitStatsGlobal { acSubmissionNum { difficulty count } } profile { ranking } } }\"}";

            URL url = new URL("https://leetcode.com/graphql");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Referer", "https://leetcode.com");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            conn.getOutputStream().write(query.getBytes());

            int status = conn.getResponseCode();
            if (status != 200) {
                HashMap<String, Object> err = new HashMap<>();
                err.put("error", "LeetCode user not found");
                return ResponseEntity.badRequest().body(err);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            return ResponseEntity.ok()
                    .header("Content-Type", "application/json")
                    .body(sb.toString());

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "LeetCode fetch failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
    // Search users by name
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (User u : users) {
            if (u.getName().toLowerCase().contains(query.toLowerCase()) ||
                    (u.getGithubUsername() != null && u.getGithubUsername().toLowerCase().contains(query.toLowerCase()))) {

                HashMap<String, Object> m = new HashMap<>();
                m.put("name", u.getName());
                m.put("score", u.getScore());
                m.put("rank", u.getRank());
                m.put("problems", u.getProblems());
                m.put("githubUsername", u.getGithubUsername() != null ? u.getGithubUsername() : "");
                m.put("leetcodeUsername", u.getLeetcodeUsername() != null ? u.getLeetcodeUsername() : "");
                result.add(m);
            }
        }
        return ResponseEntity.ok(result);
    }
}