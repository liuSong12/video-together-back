package com.videotogether.pojo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文字聊天
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMsg {
    private Integer from;
    private String message;
    private String userName;
    private String avatar;
    private String sendTime;
}
