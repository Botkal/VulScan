package com.vulscan.dashboard.dto;

import java.util.Set;

public class UserProfileDto {
    private Long id;
    private String email;
    private String status;
    private Set<String> roles;

    public UserProfileDto() {
    }

    public UserProfileDto(Long id, String email, String status, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
