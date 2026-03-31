package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.mapper.UserAccountMapper;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

    private final UserAccountMapper userAccountMapper;

    public UserAccountService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public UserAccount findByUsername(String username) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)
                .last("limit 1"));
    }
}

