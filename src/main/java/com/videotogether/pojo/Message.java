package com.videotogether.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Integer userId;
    private Object message;
    private Integer roomId;
    private String type;
    private String token;
}
