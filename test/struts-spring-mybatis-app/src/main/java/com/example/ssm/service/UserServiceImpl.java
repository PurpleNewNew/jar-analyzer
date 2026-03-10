package com.example.ssm.service;

import com.example.ssm.mapper.UserMapperImpl;

public class UserServiceImpl implements UserService {
    private final UserMapperImpl mapper = new UserMapperImpl();

    @Override
    public String search(String keyword) {
        return mapper.findUserByKeyword(keyword);
    }
}
