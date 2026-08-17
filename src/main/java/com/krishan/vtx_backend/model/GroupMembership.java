package com.krishan.vtx_backend.model;

import jakarta.persistence.*;

// Ek row = ek user ka ek group me membership. Group ki pehchan uske `code` se.
@Entity
@Table(name = "group_memberships")
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;         // group join code
    private String groupName;    // group ka naam
    private String memberEmail;  // is member ka email

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getMemberEmail() { return memberEmail; }
    public void setMemberEmail(String memberEmail) { this.memberEmail = memberEmail; }
}
