package com.vulscan.dashboard.dto;

import java.util.Set;

public class UpdateUserRolesRequestDto {
    private Set<String> roles;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
