package com.vulscan.dashboard.service;

import com.vulscan.dashboard.dto.CreateUserRequestDto;
import com.vulscan.dashboard.dto.UpdateUserRolesRequestDto;
import com.vulscan.dashboard.dto.UpdateUserStatusRequestDto;
import com.vulscan.dashboard.dto.UserProfileDto;
import com.vulscan.dashboard.entity.AppUser;
import com.vulscan.dashboard.entity.Role;
import com.vulscan.dashboard.entity.RoleName;
import com.vulscan.dashboard.entity.UserStatus;
import com.vulscan.dashboard.repository.RoleRepository;
import com.vulscan.dashboard.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserProfileDto createUser(CreateUserRequestDto request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("User already exists with this email");
        }

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(parseStatusOrDefault(request.getStatus()));
        user.setRoles(resolveRoles(request.getRoles()));

        AppUser saved = userRepository.save(user);
        return toProfile(saved);
    }

    public UserProfileDto updateUserRoles(Long userId, UpdateUserRolesRequestDto request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRoles(resolveRoles(request.getRoles()));
        AppUser saved = userRepository.save(user);
        return toProfile(saved);
    }

    public UserProfileDto updateUserStatus(Long userId, UpdateUserStatusRequestDto request) {
        if (request == null || request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(UserStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT)));
        AppUser saved = userRepository.save(user);
        return toProfile(saved);
    }

    public List<UserProfileDto> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getId))
                .map(this::toProfile)
                .collect(Collectors.toList());
    }

    public UserProfileDto getCurrentUserProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("No authenticated user");
        }

        AppUser user = userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toProfile(user);
    }

    public void ensureRoleCatalogExists() {
        Arrays.stream(RoleName.values()).forEach(this::ensureRoleExists);
    }

    private void ensureRoleExists(RoleName name) {
        roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>(Set.of(getRole(RoleName.USER)));
        }

        return roleNames.stream()
                .map(raw -> RoleName.valueOf(raw.trim().toUpperCase(Locale.ROOT)))
                .map(this::getRole)
                .collect(Collectors.toSet());
    }

    private Role getRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(new Role(name)));
    }

    private UserStatus parseStatusOrDefault(String statusRaw) {
        if (statusRaw == null || statusRaw.isBlank()) {
            return UserStatus.ACTIVE;
        }
        return UserStatus.valueOf(statusRaw.trim().toUpperCase(Locale.ROOT));
    }

    private UserProfileDto toProfile(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return new UserProfileDto(user.getId(), user.getEmail(), user.getStatus().name(), roles);
    }
}
