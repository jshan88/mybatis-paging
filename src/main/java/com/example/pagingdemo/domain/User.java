package com.example.pagingdemo.domain;

import lombok.Builder;
import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;

    @Builder
    public User(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
