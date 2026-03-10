package com.example.ssm.bootstrap;

import com.example.ssm.service.UserServiceImpl;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppBootstrapListener implements ServletContextListener {
    private final UserServiceImpl userService = new UserServiceImpl();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        userService.search("bootstrap");
    }
}
