package com.krishan.vtx_backend.repository;

import com.krishan.vtx_backend.model.ScoreSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScoreSnapshotRepository extends JpaRepository<ScoreSnapshot, Long> {
    List<ScoreSnapshot> findByUserEmailOrderBySnapDateAsc(String userEmail);
    Optional<ScoreSnapshot> findByUserEmailAndSnapDate(String userEmail, String snapDate);
}
