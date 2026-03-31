package com.graduation.fitmate.service;

import com.graduation.fitmate.entity.UserAccount;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    public CustomUserDetailsService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userAccountService.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return User.withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .disabled(!Boolean.TRUE.equals(account.getEnabled()))
                .authorities(new SimpleGrantedAuthority(account.getRole()))
                .build();
    }
}

