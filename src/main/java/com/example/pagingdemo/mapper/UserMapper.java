package com.example.pagingdemo.mapper;

import com.example.pagingdemo.domain.User;
import com.example.pagingdemo.dto.Page;
import com.example.pagingdemo.dto.PageResponse;
import com.example.pagingdemo.dto.UserParam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    public PageResponse<User> findUsersByParam(UserParam userParam);
    public PageResponse<User> findUsers(Page page);
}
