package com.krishan.vtx_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    // Optional GitHub token: agar set hai to 5000 req/hour, warna sirf 60/hour (rate-limited)
    @Value("${github.token:}")
    private String githubToken;

    // Optional Google Gemini API key for AI Career Coach (free tier)
    @Value("${gemini.api.key:}")
    private String geminiApiKey;

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
        response.put("id", user.getId());
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
            user.setGithubUsername(cleanHandle(updates.get("githubUsername")));
        if (updates.containsKey("leetcodeUsername"))
            user.setLeetcodeUsername(cleanHandle(updates.get("leetcodeUsername")));
        if (updates.containsKey("codeforcesUsername"))
            user.setCodeforcesUsername(cleanHandle(updates.get("codeforcesUsername")));
        if (updates.containsKey("hackerrankUsername"))
            user.setHackerrankUsername(cleanHandle(updates.get("hackerrankUsername")));

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

    // Username saaf karo: spaces hatao aur leading @ hatao (common typos)
    private String cleanHandle(String raw) {
        if (raw == null) return null;
        String h = raw.trim().replaceAll("\\s+", "");
        while (h.startsWith("@")) h = h.substring(1);
        return h;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        String myEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

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
            // Email se pehchano (naam se nahi) — duplicate naam pe galat highlight na ho
            m.put("isMe", u.getEmail() != null && u.getEmail().equals(myEmail));
            leaderboard.add(m);
        }
        return ResponseEntity.ok(leaderboard);
    }

    // Apna account delete karo (aur ranks dobara compute)
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);

        List<User> allUsers = userRepository.findAll();
        allUsers.sort((a, b) -> b.getScore() - a.getScore());
        for (int i = 0; i < allUsers.size(); i++) {
            allUsers.get(i).setRank(i + 1);
            userRepository.save(allUsers.get(i));
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Account deleted");
        return ResponseEntity.ok(res);
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

    // GitHub Contributions (asli heatmap) — GraphQL API, token zaroori
    @GetMapping("/github-contributions/{username}")
    public ResponseEntity<?> getGithubContributions(@PathVariable String username) {
        try {
            String query = "{\"query\":\"query { user(login: \\\"" + username
                    + "\\\") { contributionsCollection { contributionCalendar { totalContributions weeks { contributionDays { date contributionCount } } } } } }\"}";

            String body = httpPost("https://api.github.com/graphql", query, githubToken);
            JsonNode d = new ObjectMapper().readTree(body);
            JsonNode cal = d.path("data").path("user")
                    .path("contributionsCollection").path("contributionCalendar");

            if (cal.isMissingNode() || cal.path("weeks").isMissingNode()) {
                HashMap<String, Object> err = new HashMap<>();
                err.put("error", "GitHub contributions not available");
                return ResponseEntity.status(404).body(err);
            }

            List<Map<String, Object>> days = new ArrayList<>();
            for (JsonNode week : cal.path("weeks")) {
                for (JsonNode day : week.path("contributionDays")) {
                    HashMap<String, Object> m = new HashMap<>();
                    m.put("date", day.path("date").asText(""));
                    m.put("count", day.path("contributionCount").asInt(0));
                    days.add(m);
                }
            }

            HashMap<String, Object> res = new HashMap<>();
            res.put("totalContributions", cal.path("totalContributions").asInt(0));
            res.put("days", days);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "GitHub contributions fetch failed: " + e.getMessage());
            return ResponseEntity.status(502).body(err);
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
    // Codeforces Real Data API
    @GetMapping("/codeforces/{username}")
    public ResponseEntity<?> getCodeforcesUser(@PathVariable String username) {
        try {
            ObjectMapper om = new ObjectMapper();

            // 1) Rating / rank
            String infoBody = httpGet("https://codeforces.com/api/user.info?handles=" + username);
            JsonNode info = om.readTree(infoBody);
            if (!"OK".equals(info.path("status").asText())) {
                HashMap<String, Object> err = new HashMap<>();
                err.put("error", "Codeforces user not found");
                return ResponseEntity.status(404).body(err);
            }
            JsonNode u = info.path("result").get(0);

            HashMap<String, Object> res = new HashMap<>();
            res.put("handle", u.path("handle").asText(username));
            res.put("rating", u.path("rating").asInt(0));
            res.put("maxRating", u.path("maxRating").asInt(0));
            res.put("rank", u.path("rank").asText(""));
            res.put("maxRank", u.path("maxRank").asText(""));

            // 2) Solved count (best-effort, last 10k submissions to bound memory)
            int solved = 0;
            try {
                String statusBody = httpGet(
                        "https://codeforces.com/api/user.status?handle=" + username + "&from=1&count=10000");
                JsonNode st = om.readTree(statusBody);
                if ("OK".equals(st.path("status").asText())) {
                    Set<String> done = new HashSet<>();
                    for (JsonNode s : st.path("result")) {
                        if ("OK".equals(s.path("verdict").asText())) {
                            JsonNode p = s.path("problem");
                            done.add(p.path("contestId").asText() + p.path("index").asText());
                        }
                    }
                    solved = done.size();
                }
            } catch (Exception ignore) { /* rating abhi bhi return ho jayega */ }
            res.put("solved", solved);

            return ResponseEntity.ok(res);

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "Codeforces fetch failed: " + e.getMessage());
            return ResponseEntity.status(502).body(err);
        }
    }

    // HackerRank Real Data API (badges)
    @GetMapping("/hackerrank/{username}")
    public ResponseEntity<?> getHackerrankUser(@PathVariable String username) {
        try {
            ObjectMapper om = new ObjectMapper();
            String body = httpGet("https://www.hackerrank.com/rest/hackers/" + username + "/badges");
            JsonNode d = om.readTree(body);

            if (!d.path("status").asBoolean(false)) {
                HashMap<String, Object> err = new HashMap<>();
                err.put("error", "HackerRank user not found");
                return ResponseEntity.status(404).body(err);
            }

            List<Map<String, Object>> badges = new ArrayList<>();
            int totalStars = 0;
            int totalSolved = 0;
            for (JsonNode m : d.path("models")) {
                HashMap<String, Object> b = new HashMap<>();
                b.put("name", m.path("badge_name").asText(""));
                int stars = m.path("total_stars").asInt(0);
                int bSolved = m.path("solved").asInt(0);
                b.put("stars", stars);
                b.put("solved", bSolved);
                badges.add(b);
                totalStars += stars;
                totalSolved += bSolved;
            }

            HashMap<String, Object> res = new HashMap<>();
            res.put("username", username);
            res.put("badges", badges);
            res.put("totalStars", totalStars);
            res.put("totalSolved", totalSolved);
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "HackerRank fetch failed: " + e.getMessage());
            return ResponseEntity.status(502).body(err);
        }
    }

    // Auto-sync: connected platforms ka real data laakar Dev Score + problems compute karo,
    // save karo, aur sabki rank dobara nikaalo. Manual entry ki zaroorat khatam.
    @PutMapping("/sync")
    public ResponseEntity<?> syncStats() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int score = 0;
        int problems = 0;
        HashMap<String, Object> breakdown = new HashMap<>();
        // Breakdown ke saare platforms 0 se shuru (frontend chart ke liye consistent)
        breakdown.put("github", 0);
        breakdown.put("leetcode", 0);
        breakdown.put("codeforces", 0);
        breakdown.put("hackerrank", 0);

        // LeetCode
        if (user.getLeetcodeUsername() != null && !user.getLeetcodeUsername().isBlank()) {
            try {
                int[] lc = leetcodeStats(user.getLeetcodeUsername());
                score += lc[0];
                problems += lc[1];
                breakdown.put("leetcode", lc[0]);
            } catch (Exception ignore) { }
        }
        // Codeforces
        if (user.getCodeforcesUsername() != null && !user.getCodeforcesUsername().isBlank()) {
            try {
                int[] cf = codeforcesStats(user.getCodeforcesUsername());
                score += cf[0];
                problems += cf[1];
                breakdown.put("codeforces", cf[0]);
            } catch (Exception ignore) { }
        }
        // GitHub (score + real streak from contribution calendar)
        if (user.getGithubUsername() != null && !user.getGithubUsername().isBlank()) {
            try {
                int gh = githubScore(user.getGithubUsername());
                score += gh;
                breakdown.put("github", gh);
            } catch (Exception ignore) { }
            try {
                user.setStreak(githubStreak(user.getGithubUsername()));
            } catch (Exception ignore) { }
        }
        // HackerRank
        if (user.getHackerrankUsername() != null && !user.getHackerrankUsername().isBlank()) {
            try {
                int[] hr = hackerrankStats(user.getHackerrankUsername());
                score += hr[0];
                problems += hr[1];
                breakdown.put("hackerrank", hr[0]);
            } catch (Exception ignore) { }
        }

        user.setScore(score);
        user.setProblems(problems);
        userRepository.save(user);

        // Sabki rank dobara compute karo
        List<User> allUsers = userRepository.findAll();
        allUsers.sort((a, b) -> b.getScore() - a.getScore());
        for (int i = 0; i < allUsers.size(); i++) {
            allUsers.get(i).setRank(i + 1);
            userRepository.save(allUsers.get(i));
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("message", "Stats synced from your platforms!");
        res.put("score", user.getScore());
        res.put("problems", user.getProblems());
        res.put("rank", user.getRank());
        res.put("breakdown", breakdown);
        return ResponseEntity.ok(res);
    }

    // Chhota helper: GET request + response body string (2xx ho ya error, dono padho)
    private String httpGet(String urlStr) throws IOException {
        return httpGet(urlStr, null);
    }

    private String httpGet(String urlStr, String bearer) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept", "application/json");
        if (bearer != null && !bearer.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
        }
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // POST helper. bearer null ho to LeetCode ke liye (Referer set), warna GitHub GraphQL (auth token)
    private String httpPost(String urlStr, String jsonBody) throws IOException {
        return httpPost(urlStr, jsonBody, null);
    }

    private String httpPost(String urlStr, String jsonBody, String bearer) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        if (bearer != null && !bearer.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + bearer);
        } else {
            conn.setRequestProperty("Referer", "https://leetcode.com");
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.getOutputStream().write(jsonBody.getBytes());

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ---- Per-platform score calculators (used by /sync). Har ek {score, solved} deta hai ----

    private int[] leetcodeStats(String username) throws IOException {
        String query = "{\"query\":\"{ matchedUser(username: \\\"" + username
                + "\\\") { submitStats: submitStatsGlobal { acSubmissionNum { difficulty count } } } }\"}";
        JsonNode d = new ObjectMapper().readTree(httpPost("https://leetcode.com/graphql", query));
        JsonNode arr = d.path("data").path("matchedUser").path("submitStats").path("acSubmissionNum");
        int easy = 0, med = 0, hard = 0, total = 0;
        for (JsonNode n : arr) {
            int c = n.path("count").asInt(0);
            switch (n.path("difficulty").asText("")) {
                case "Easy": easy = c; break;
                case "Medium": med = c; break;
                case "Hard": hard = c; break;
                case "All": total = c; break;
                default: break;
            }
        }
        int score = easy * 10 + med * 30 + hard * 50;
        return new int[]{score, total};
    }

    private int[] codeforcesStats(String username) throws IOException {
        ObjectMapper om = new ObjectMapper();
        JsonNode info = om.readTree(httpGet("https://codeforces.com/api/user.info?handles=" + username));
        if (!"OK".equals(info.path("status").asText())) return new int[]{0, 0};
        int rating = info.path("result").get(0).path("rating").asInt(0);

        int solved = 0;
        try {
            JsonNode st = om.readTree(httpGet(
                    "https://codeforces.com/api/user.status?handle=" + username + "&from=1&count=10000"));
            if ("OK".equals(st.path("status").asText())) {
                Set<String> done = new HashSet<>();
                for (JsonNode s : st.path("result")) {
                    if ("OK".equals(s.path("verdict").asText())) {
                        JsonNode p = s.path("problem");
                        done.add(p.path("contestId").asText() + p.path("index").asText());
                    }
                }
                solved = done.size();
            }
        } catch (Exception ignore) { /* rating se score to mil hi jayega */ }

        int score = solved * 20 + Math.max(0, rating - 1000);
        return new int[]{score, solved};
    }

    private int githubScore(String username) throws IOException {
        JsonNode d = new ObjectMapper().readTree(httpGet("https://api.github.com/users/" + username, githubToken));
        int repos = d.path("public_repos").asInt(0);
        int followers = d.path("followers").asInt(0);
        return repos * 5 + followers * 10;
    }

    // Current streak = aaj/kal se peeche lagataar kitne din contribution hai
    private int githubStreak(String username) throws IOException {
        String query = "{\"query\":\"query { user(login: \\\"" + username
                + "\\\") { contributionsCollection { contributionCalendar { weeks { contributionDays { contributionCount } } } } } }\"}";
        JsonNode cal = new ObjectMapper().readTree(httpPost("https://api.github.com/graphql", query, githubToken))
                .path("data").path("user").path("contributionsCollection").path("contributionCalendar");

        List<Integer> counts = new ArrayList<>();
        for (JsonNode week : cal.path("weeks")) {
            for (JsonNode day : week.path("contributionDays")) {
                counts.add(day.path("contributionCount").asInt(0));
            }
        }

        int i = counts.size() - 1;
        int streak = 0;
        if (i >= 0 && counts.get(i) == 0) i--; // aaj ka din 0 ho to skip (abhi tak commit nahi kiya)
        while (i >= 0 && counts.get(i) > 0) { streak++; i--; }
        return streak;
    }

    private int[] hackerrankStats(String username) throws IOException {
        JsonNode d = new ObjectMapper().readTree(
                httpGet("https://www.hackerrank.com/rest/hackers/" + username + "/badges"));
        int stars = 0, solved = 0;
        for (JsonNode m : d.path("models")) {
            stars += m.path("total_stars").asInt(0);
            solved += m.path("solved").asInt(0);
        }
        int score = stars * 20 + solved * 5;
        return new int[]{score, solved};
    }

    // Real AI Career Coach — Google Gemini (free) se personalized insights
    @PostMapping("/ai-coach")
    public ResponseEntity<?> aiCoach(@RequestBody(required = false) Map<String, Object> reqBody) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "AI coach not configured");
            return ResponseEntity.status(503).body(err);
        }

        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Stats summary banao
        StringBuilder stats = new StringBuilder();
        stats.append("Name: ").append(user.getName()).append("\n");
        stats.append("Dev Score: ").append(user.getScore()).append("\n");
        stats.append("Global Rank: #").append(user.getRank()).append("\n");
        stats.append("Total problems solved: ").append(user.getProblems()).append("\n");
        stats.append("Current streak: ").append(user.getStreak()).append(" days\n");
        stats.append("Connected platforms: ");
        List<String> platforms = new ArrayList<>();
        if (user.getGithubUsername() != null && !user.getGithubUsername().isBlank()) platforms.add("GitHub");
        if (user.getLeetcodeUsername() != null && !user.getLeetcodeUsername().isBlank()) platforms.add("LeetCode");
        if (user.getCodeforcesUsername() != null && !user.getCodeforcesUsername().isBlank()) platforms.add("Codeforces");
        if (user.getHackerrankUsername() != null && !user.getHackerrankUsername().isBlank()) platforms.add("HackerRank");
        stats.append(platforms.isEmpty() ? "none" : String.join(", ", platforms)).append("\n");
        if (reqBody != null && reqBody.get("breakdown") != null) {
            stats.append("Score breakdown by platform: ").append(reqBody.get("breakdown")).append("\n");
        }

        String system = "You are an expert developer career coach. Given a developer's real coding stats "
                + "across GitHub, LeetCode, Codeforces and HackerRank, give sharp, specific, encouraging career advice.";
        String userPrompt = "Here are the developer's real stats:\n" + stats
                + "\nGive: (1) their single biggest strength, (2) the most important area to improve, "
                + "(3) two concrete next steps, (4) a one-line interview-readiness verdict. "
                + "Address them as 'you'. Keep it under 180 words, use short bullet points.";

        try {
            ObjectMapper om = new ObjectMapper();
            ObjectNode root = om.createObjectNode();

            // System instruction
            ObjectNode sysInstr = root.putObject("systemInstruction");
            sysInstr.putArray("parts").addObject().put("text", system);

            // User content
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            content.put("role", "user");
            content.putArray("parts").addObject().put("text", userPrompt);

            // Generation config — thinking off + bigger budget so the answer isn't truncated
            ObjectNode genConfig = root.putObject("generationConfig");
            genConfig.put("maxOutputTokens", 1200);
            genConfig.put("temperature", 0.7);
            genConfig.putObject("thinkingConfig").put("thinkingBudget", 0);

            String respBody = geminiPost(om.writeValueAsString(root));
            JsonNode d = om.readTree(respBody);

            StringBuilder out = new StringBuilder();
            for (JsonNode part : d.path("candidates").path(0).path("content").path("parts")) {
                out.append(part.path("text").asText());
            }
            if (out.length() == 0) {
                HashMap<String, Object> err = new HashMap<>();
                err.put("error", "AI returned no text: " + d.path("error").path("message").asText("unknown"));
                return ResponseEntity.status(502).body(err);
            }

            HashMap<String, Object> res = new HashMap<>();
            res.put("insights", out.toString());
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            HashMap<String, Object> err = new HashMap<>();
            err.put("error", "AI request failed: " + e.getMessage());
            return ResponseEntity.status(502).body(err);
        }
    }

    // Google Gemini generateContent POST (API key as query param)
    private String geminiPost(String jsonBody) throws IOException {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key="
                + geminiApiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(90000);
        conn.getOutputStream().write(jsonBody.getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 400) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
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