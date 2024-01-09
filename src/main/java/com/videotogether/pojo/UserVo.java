package com.videotogether.pojo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVo {
    private Integer id;
    private String userName;
    private String avatar;
    private LocalDateTime createTime;
    private LocalDateTime lastTogetherWatch;
    private LocalDateTime lastUpdateTime;


    private Object offer;
    private Object answer;
    private Object candidate;
}
