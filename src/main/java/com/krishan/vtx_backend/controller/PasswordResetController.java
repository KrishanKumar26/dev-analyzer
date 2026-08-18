package com.krishan.vtx_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.krishan.vtx_backend.model.PasswordResetToken;
import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.PasswordResetTokenRepository;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import java.net.HttpURLConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Password reset via email (Resend). Public endpoints (under /api/auth, permitAll).
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${resend.api.key:}")
    private String resendKey;
    @Value("${app.mail.from:Dev Analyzer <onboarding@resend.dev>}")
    private String mailFrom;
    @Value("${app.frontend.url:https://dev-analyzer-frontend.vercel.app}")
    private String frontendUrl;

    public PasswordResetController(UserRepository userRepository,
                                  PasswordResetTokenRepository tokenRepo,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        if (resendKey == null || resendKey.isBlank()) {
            return ResponseEntity.status(503).body(Map.of("error", "Email service not configured"));
        }
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email required"));
        }
        email = email.trim();

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isPresent()) {
            // Purane token hatao
            tokenRepo.deleteAll(tokenRepo.findByEmail(email));
            String token = UUID.randomUUID().toString().replace("-", "");
            PasswordResetToken prt = new PasswordResetToken();
            prt.setEmail(email);
            prt.setToken(token);
            prt.setExpiresAt(System.currentTimeMillis() + 30 * 60 * 1000L); // 30 min
            tokenRepo.save(prt);

            String link = frontendUrl + "/reset?token=" + token;
            String html = "<div style=\"font-family:sans-serif;max-width:480px;margin:auto\">"
                    + "<h2>Reset your Dev Analyzer password</h2>"
                    + "<p>Hi " + escape(opt.get().getName()) + ", click below to set a new password. Link expires in 30 minutes.</p>"
                    + "<p><a href=\"" + link + "\" style=\"background:#059669;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;display:inline-block\">Reset Password</a></p>"
                    + "<p style=\"color:#888;font-size:12px\">Agar tumne request nahi kiya to ignore karo.</p>"
                    + "<p style=\"color:#888;font-size:12px\">Or paste this link: " + link + "</p></div>";
            try {
                sendEmail(email, "Reset your Dev Analyzer password", html);
            } catch (Exception e) {
                return ResponseEntity.status(502).body(Map.of("error", "Failed to send email: " + e.getMessage()));
            }
        }
        // Security: email exist kare ya na kare, same generic response
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String password = body.get("password");
        if (token == null || password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Token and a password (6+ chars) required"));
        }
        Optional<PasswordResetToken> opt = tokenRepo.findByToken(token.trim());
        if (opt.isEmpty() || opt.get().getExpiresAt() < System.currentTimeMillis()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired reset link"));
        }
        PasswordResetToken prt = opt.get();
        Optional<User> userOpt = userRepository.findByEmail(prt.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        tokenRepo.delete(prt);
        return ResponseEntity.ok(Map.of("message", "Password reset! Ab naye password se login karo."));
    }

    private void sendEmail(String to, String subject, String html) throws Exception {
        ObjectMapper om = new ObjectMapper();
        ObjectNode root = om.createObjectNode();
        root.put("from", mailFrom);
        root.putArray("to").add(to);
        root.put("subject", subject);
        root.put("html", html);

        URL url = new URL("https://api.resend.com/emails");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + resendKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(20000);
        conn.getOutputStream().write(om.writeValueAsString(root).getBytes(StandardCharsets.UTF_8));

        int status = conn.getResponseCode();
        if (status >= 400) {
            InputStream is = conn.getErrorStream();
            StringBuilder sb = new StringBuilder();
            if (is != null) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line; while ((line = r.readLine()) != null) sb.append(line);
                }
            }
            throw new RuntimeException("Resend HTTP " + status + ": " + sb);
        }
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("<", "&lt;").replace(">", "&gt;");
    }
}
