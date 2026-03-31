package com.graduation.fitmate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graduation.fitmate.entity.UserAccount;
import com.graduation.fitmate.entity.UserProfile;
import com.graduation.fitmate.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final UserAccountService userAccountService;

    public UserProfileService(UserProfileMapper userProfileMapper, UserAccountService userAccountService) {
        this.userProfileMapper = userProfileMapper;
        this.userAccountService = userAccountService;
    }

    public UserProfile getProfileByUsername(String username) {
        UserAccount account = userAccountService.findByUsername(username);
        if (account == null) {
            return null;
        }
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, account.getId())
                .last("limit 1"));
    }

    @Transactional
    public UserProfile saveProfile(String username, UserProfile form) {
        UserAccount account = userAccountService.findByUsername(username);
        UserProfile existing = getProfileByUsername(username);
        if (existing == null) {
            form.setUserId(account.getId());
            if (form.getKneeSensitive() == null) {
                form.setKneeSensitive(false);
            }
            if (form.getBackSensitive() == null) {
                form.setBackSensitive(false);
            }
            userProfileMapper.insert(form);
            return form;
        }
        form.setId(existing.getId());
        form.setUserId(existing.getUserId());
        if (form.getKneeSensitive() == null) {
            form.setKneeSensitive(false);
        }
        if (form.getBackSensitive() == null) {
            form.setBackSensitive(false);
        }
        userProfileMapper.updateById(form);
        return form;
    }
}

