package com.rd.backend.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rd.backend.mapper.UserMapper;
import com.rd.backend.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;   // 仅保留这一条依赖

    @Override
    public UserDetails loadUserByUsername(String userAccount) throws UsernameNotFoundException {

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUserAccount, userAccount)
                        .eq(User::getIsDelete, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserAccount())
                .password(user.getUserPassword())
                .authorities(user.getUserRole())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
