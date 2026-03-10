package com.example.ssm.service;

import com.example.ssm.mapper.UserMapperImpl;
import com.example.ssm.sink.SearchAuditSink;

public class UserServiceImpl implements UserService {
    private final UserMapperImpl mapper = new UserMapperImpl();

    @Override
    public String search(String keyword) {
        new SearchAuditSink().record(keyword);
        return mapper.findUserByKeyword(keyword);
    }
}
