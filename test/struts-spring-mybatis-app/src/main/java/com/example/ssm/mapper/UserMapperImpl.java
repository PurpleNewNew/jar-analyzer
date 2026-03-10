package com.example.ssm.mapper;

public class UserMapperImpl implements UserMapper {
    @Override
    public String findUserByKeyword(String keyword) {
        return keyword == null ? "guest" : keyword;
    }
}
