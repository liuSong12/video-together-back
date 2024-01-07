package com.videotogether.pojo;

import lombok.Data;

@Data
public class UserVo {
    private Integer id;
    private String userName;
    private String avatar;

    private Object offer;
    private Object answer;
    private Object candidate;
}
