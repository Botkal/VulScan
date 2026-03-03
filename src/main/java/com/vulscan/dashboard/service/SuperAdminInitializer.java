package com.vulscan.dashboard.service;

import com.vulscan.dashboard.entity.AppUser;
import com.vulscan.dashboard.entity.RoleName;
import com.vulscan.dashboard.entity.UserStatus;
import com.vulscan.dashboard.repository.RoleRepository;
import com.vulscan.dashboard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserManagementService userManagementService;

    @Value("${app.superadmin.enabled:true}")
    private boolean superAdminEnabled;

    @Value("${app.superadmin.email:superadmin@vulscan.local}")
    private String superAdminEmail;

    @Value("${app.superadmin.password:SuperAdmin123!}")
    private String superAdminPassword;

    public SuperAdminInitializer(UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 UserManagementService userManagementService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userManagementService = userManagementService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!superAdminEnabled) {
            return;
        }

        userManagementService.ensureRoleCatalogExists();

        var requiredRoles = new HashSet<>(Set.of(
                roleRepository.findByName(RoleName.USER).orElseThrow(),
                roleRepository.findByName(RoleName.ADMIN).orElseThrow(),
                roleRepository.findByName(RoleName.SUPERADMIN).orElseThrow()
        ));

        userRepository.findByEmailIgnoreCase(superAdminEmail).ifPresent(existing -> {
            existing.setStatus(UserStatus.ACTIVE);
            existing.setRoles(requiredRoles);
            userRepository.save(existing);
        });

        if (userRepository.count() > 0) {
            return;
        }

        userRepository.findByEmailIgnoreCase(superAdminEmail).orElseGet(() -> {
            AppUser user = new AppUser();
            user.setEmail(superAdminEmail.toLowerCase(Locale.ROOT));
            user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
            user.setStatus(UserStatus.ACTIVE);
            user.setRoles(requiredRoles);
            return userRepository.save(user);
        });
    }
}
