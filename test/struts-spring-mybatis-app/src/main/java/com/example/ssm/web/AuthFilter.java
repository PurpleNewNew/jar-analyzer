package com.example.ssm.web;

import com.example.ssm.service.UserServiceImpl;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class AuthFilter implements Filter {
    private final UserServiceImpl userService = new UserServiceImpl();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        userService.search(request.getParameter("keyword"));
        if (chain != null) {
            chain.doFilter(request, response);
        }
    }
}
