package com.krishan.vtx_backend.repository;

import com.krishan.vtx_backend.model.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByMemberEmail(String memberEmail);
    List<GroupMembership> findByCode(String code);
    Optional<GroupMembership> findByCodeAndMemberEmail(String code, String memberEmail);
}
