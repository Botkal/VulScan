package com.vulscan.dashboard.controller;

import com.vulscan.dashboard.dto.CreateUserRequestDto;
import com.vulscan.dashboard.dto.UpdateUserRolesRequestDto;
import com.vulscan.dashboard.dto.UpdateUserStatusRequestDto;
import com.vulscan.dashboard.dto.UserProfileDto;
import com.vulscan.dashboard.service.UserManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<List<UserProfileDto>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UserProfileDto> createUser(@RequestBody CreateUserRequestDto request) {
        return ResponseEntity.ok(userManagementService.createUser(request));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UserProfileDto> updateUserRoles(@PathVariable("id") Long userId,
                                                          @RequestBody UpdateUserRolesRequestDto request) {
        return ResponseEntity.ok(userManagementService.updateUserRoles(userId, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
    public ResponseEntity<UserProfileDto> updateUserStatus(@PathVariable("id") Long userId,
                                                           @RequestBody UpdateUserStatusRequestDto request) {
        return ResponseEntity.ok(userManagementService.updateUserStatus(userId, request));
    }
}
