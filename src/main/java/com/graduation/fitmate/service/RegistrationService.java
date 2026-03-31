package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.dto.RegisterRequest;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.mapper.UserAccountMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        Long existing = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, request.getUsername()));
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("Username already exists.");
        }
        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setDisplayName(request.getDisplayName());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole("ROLE_USER");
        account.setEnabled(true);
        userAccountMapper.insert(account);
    }
}
