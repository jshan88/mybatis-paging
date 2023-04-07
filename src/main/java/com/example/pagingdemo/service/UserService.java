package com.example.pagingdemo.service;

import com.example.pagingdemo.domain.User;
import com.example.pagingdemo.dto.Page;
import com.example.pagingdemo.dto.PageResponse;
import com.example.pagingdemo.dto.UserParam;
import com.example.pagingdemo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    public PageResponse<User> findUsersWithParams(UserParam userParam) {
        PageResponse<User> users = userMapper.findUsersByParam(userParam);

        return users;
    }

    public PageResponse<User> findUsers(Page page) {
        PageResponse<User> users = userMapper.findUsers(page);

        return users;
    }

}
