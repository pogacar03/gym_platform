package com.graduation.fitmate.service;

import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.mapper.UserAccountMapper;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminCredentialInitializer {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;

    public AdminCredentialInitializer(
            UserAccountMapper userAccountMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.admin-password:}") String adminPassword
    ) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void updateAdminPasswordIfConfigured() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD is not configured. Demo admin credentials may still be active.");
            return;
        }
        UserAccount admin = userAccountMapper.selectById(2L);
        if (admin == null || !"admin".equals(admin.getUsername())) {
            admin = new com.graduation.fitmate.service.UserAccountService(userAccountMapper).findByUsername("admin");
        }
        if (admin == null) {
            log.warn("Could not update admin password because admin account was not found.");
            return;
        }
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setUpdatedAt(LocalDateTime.now());
        userAccountMapper.updateById(admin);
        log.info("Admin password updated from ADMIN_PASSWORD configuration.");
    }
}
