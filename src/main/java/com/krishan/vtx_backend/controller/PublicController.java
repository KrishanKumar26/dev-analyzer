package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    // Embeddable README badge (SVG) — shields.io style. Public, no auth.
    @GetMapping(value = "/badge/{id}.svg", produces = "image/svg+xml")
    public ResponseEntity<String> badge(@PathVariable Long id) {
        Optional<User> opt = userRepository.findById(id);
        int score = opt.map(User::getScore).orElse(0);
        String rank = opt.map(u -> "#" + u.getRank()).orElse("#-");

        String label = "DEV SCORE";
        String value = String.valueOf(score) + "  ·  " + rank;
        int labelW = 12 + label.length() * 7;
        int valueW = 14 + value.length() * 7;
        int total = labelW + valueW;
        int labelX = labelW / 2;
        int valueX = labelW + valueW / 2;

        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + total + "\" height=\"20\" role=\"img\" aria-label=\"Dev Score " + value + "\">"
                + "<linearGradient id=\"s\" x2=\"0\" y2=\"100%\"><stop offset=\"0\" stop-color=\"#fff\" stop-opacity=\".08\"/><stop offset=\"1\" stop-opacity=\".12\"/></linearGradient>"
                + "<clipPath id=\"r\"><rect width=\"" + total + "\" height=\"20\" rx=\"4\" fill=\"#fff\"/></clipPath>"
                + "<g clip-path=\"url(#r)\">"
                + "<rect width=\"" + labelW + "\" height=\"20\" fill=\"#1f2937\"/>"
                + "<rect x=\"" + labelW + "\" width=\"" + valueW + "\" height=\"20\" fill=\"#059669\"/>"
                + "<rect width=\"" + total + "\" height=\"20\" fill=\"url(#s)\"/>"
                + "</g>"
                + "<g fill=\"#fff\" text-anchor=\"middle\" font-family=\"Verdana,Geneva,DejaVu Sans,sans-serif\" font-size=\"11\">"
                + "<text x=\"" + labelX + "\" y=\"14\">" + label + "</text>"
                + "<text x=\"" + valueX + "\" y=\"14\">" + value + "</text>"
                + "</g></svg>";

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(svg);
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
