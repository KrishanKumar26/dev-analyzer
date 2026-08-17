package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Public (bina login) developer profile — shareable link ke liye
@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final UserRepository userRepository;

    public PublicController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> publicProfile(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "Profile not found");
            return ResponseEntity.status(404).body(err);
        }
        User u = opt.get();

        HashMap<String, Object> res = new HashMap<>();
        res.put("name", u.getName());
        res.put("score", u.getScore());
        res.put("rank", u.getRank());
        res.put("streak", u.getStreak());
        res.put("problems", u.getProblems());

        List<Map<String, Object>> platforms = new ArrayList<>();
        addPlatform(platforms, "GitHub", u.getGithubUsername(), "https://github.com/");
        addPlatform(platforms, "LeetCode", u.getLeetcodeUsername(), "https://leetcode.com/u/");
        addPlatform(platforms, "Codeforces", u.getCodeforcesUsername(), "https://codeforces.com/profile/");
        addPlatform(platforms, "HackerRank", u.getHackerrankUsername(), "https://www.hackerrank.com/profile/");
        res.put("platforms", platforms);

        return ResponseEntity.ok(res);
    }

    private void addPlatform(List<Map<String, Object>> list, String name, String username, String baseUrl) {
        if (username != null && !username.isBlank()) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("platform", name);
            m.put("username", username);
            m.put("url", baseUrl + username);
            list.add(m);
        }
    }
}
