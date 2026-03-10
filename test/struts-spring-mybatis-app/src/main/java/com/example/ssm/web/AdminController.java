package com.example.ssm.web;

import com.example.ssm.service.UserServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private final UserServiceImpl userService = new UserServiceImpl();

    @GetMapping("/users")
    public String search(String keyword) {
        return userService.search(keyword);
    }
}
