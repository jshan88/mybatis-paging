package com.example.pagingdemo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Page {
    private Integer pageNumber = 1;

    private Integer pageSize = 10;
}
