package com.krishan.vtx_backend.service;

import com.krishan.vtx_backend.dto.AuthResponse;
import com.krishan.vtx_backend.dto.LoginRequest;
import com.krishan.vtx_backend.dto.RegisterRequest;
import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.UserRepository;
import com.krishan.vtx_backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setGithubUsername(req.getGithubUsername());
        user.setLeetcodeUsername(req.getLeetcodeUsername());
        user.setScore(0);
        user.setRank(9999);
        user.setStreak(0);
        user.setProblems(0);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(),
                user.getScore(), user.getRank(), user.getStreak());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Wrong password!");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(),
                user.getScore(), user.getRank(), user.getStreak());
    }
}