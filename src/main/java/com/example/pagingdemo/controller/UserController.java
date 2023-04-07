package com.example.pagingdemo.controller;

import com.example.pagingdemo.domain.User;
import com.example.pagingdemo.dto.Page;
import com.example.pagingdemo.dto.PageResponse;
import com.example.pagingdemo.dto.UserParam;
import com.example.pagingdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class UserController {

    private final UserService userService;

    @GetMapping("/users")
    public PageResponse<User> findUsersWithParams(@ModelAttribute UserParam userParam) {
        return userService.findUsersWithParams(userParam);
    }

    @GetMapping("/api/users")
    public PageResponse<User> findUsers(Page page) {
        return userService.findUsers(page);
    }
}
