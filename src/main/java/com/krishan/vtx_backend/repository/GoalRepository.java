package com.krishan.vtx_backend.repository;

import com.krishan.vtx_backend.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByUserEmail(String userEmail);
    Optional<Goal> findByIdAndUserEmail(Long id, String userEmail);
}
