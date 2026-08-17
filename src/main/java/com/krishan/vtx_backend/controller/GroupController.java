package com.krishan.vtx_backend.controller;

import com.krishan.vtx_backend.model.GroupMembership;
import com.krishan.vtx_backend.model.User;
import com.krishan.vtx_backend.repository.GroupMembershipRepository;
import com.krishan.vtx_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Groups / Leagues — apne college/company/friends ke private leaderboards
@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupMembershipRepository groupRepo;
    private final UserRepository userRepository;

    public GroupController(GroupMembershipRepository groupRepo, UserRepository userRepository) {
        this.groupRepo = groupRepo;
        this.userRepository = userRepository;
    }

    private String myEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // Naya group banao
    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Group name required"));
        }
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        GroupMembership m = new GroupMembership();
        m.setCode(code);
        m.setGroupName(name.trim());
        m.setMemberEmail(myEmail());
        groupRepo.save(m);

        HashMap<String, Object> res = new HashMap<>();
        res.put("code", code);
        res.put("name", name.trim());
        res.put("message", "Group created!");
        return ResponseEntity.ok(res);
    }

    // Code se group join karo
    @PostMapping("/join")
    public ResponseEntity<?> joinGroup(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Group code required"));
        }
        code = code.trim().toUpperCase();

        List<GroupMembership> members = groupRepo.findByCode(code);
        if (members.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Group not found"));
        }

        String email = myEmail();
        Optional<GroupMembership> already = groupRepo.findByCodeAndMemberEmail(code, email);
        String groupName = members.get(0).getGroupName();
        if (already.isEmpty()) {
            GroupMembership m = new GroupMembership();
            m.setCode(code);
            m.setGroupName(groupName);
            m.setMemberEmail(email);
            groupRepo.save(m);
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("code", code);
        res.put("name", groupName);
        res.put("message", "Joined group!");
        return ResponseEntity.ok(res);
    }

    // Mere groups
    @GetMapping("/mine")
    public ResponseEntity<?> myGroups() {
        List<GroupMembership> mine = groupRepo.findByMemberEmail(myEmail());
        List<Map<String, Object>> out = new ArrayList<>();
        for (GroupMembership m : mine) {
            HashMap<String, Object> g = new HashMap<>();
            g.put("code", m.getCode());
            g.put("name", m.getGroupName());
            g.put("members", groupRepo.findByCode(m.getCode()).size());
            out.add(g);
        }
        return ResponseEntity.ok(out);
    }

    // Group ka leaderboard
    @GetMapping("/{code}/leaderboard")
    public ResponseEntity<?> groupLeaderboard(@PathVariable String code) {
        code = code.trim().toUpperCase();
        List<GroupMembership> members = groupRepo.findByCode(code);
        if (members.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Group not found"));
        }
        String myEmail = myEmail();

        List<User> users = new ArrayList<>();
        for (GroupMembership m : members) {
            userRepository.findByEmail(m.getMemberEmail()).ifPresent(users::add);
        }
        users.sort((a, b) -> b.getScore() - a.getScore());

        List<Map<String, Object>> board = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            HashMap<String, Object> row = new HashMap<>();
            row.put("name", u.getName());
            row.put("score", u.getScore());
            row.put("problems", u.getProblems());
            row.put("rank", i + 1);
            row.put("isMe", u.getEmail() != null && u.getEmail().equals(myEmail));
            board.add(row);
        }

        HashMap<String, Object> res = new HashMap<>();
        res.put("name", members.get(0).getGroupName());
        res.put("code", code);
        res.put("leaderboard", board);
        return ResponseEntity.ok(res);
    }

    // Group chhodo
    @DeleteMapping("/{code}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable String code) {
        final String c = code.trim().toUpperCase();
        groupRepo.findByCodeAndMemberEmail(c, myEmail()).ifPresent(groupRepo::delete);
        return ResponseEntity.ok(Map.of("message", "Left group"));
    }
}
