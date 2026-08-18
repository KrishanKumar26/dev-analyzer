package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.Goal;
import com.krishan.vtx_backend.repository.GoalRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Goals tracker — apne coding goals set karo aur progress track karo
@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalRepository goalRepo;

    public GoalController(GoalRepository goalRepo) {
        this.goalRepo = goalRepo;
    }

    private String myEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @GetMapping
    public ResponseEntity<?> myGoals() {
        return ResponseEntity.ok(goalRepo.findByUserEmail(myEmail()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String title = body.get("title") == null ? "" : body.get("title").toString().trim();
        if (title.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title required"));
        }
        int target = 1;
        try { target = Integer.parseInt(String.valueOf(body.getOrDefault("target", 1))); } catch (Exception ignore) {}
        if (target < 1) target = 1;

        Goal g = new Goal();
        g.setUserEmail(myEmail());
        g.setTitle(title);
        g.setTarget(target);
        g.setCurrent(0);
        return ResponseEntity.ok(goalRepo.save(g));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return goalRepo.findByIdAndUserEmail(id, myEmail()).map(g -> {
            if (body.get("current") != null) {
                try { g.setCurrent(Math.max(0, Integer.parseInt(String.valueOf(body.get("current"))))); } catch (Exception ignore) {}
            }
            return ResponseEntity.ok(goalRepo.save(g));
        }).orElse(ResponseEntity.status(404).body(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        goalRepo.findByIdAndUserEmail(id, myEmail()).ifPresent(goalRepo::delete);
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
}
