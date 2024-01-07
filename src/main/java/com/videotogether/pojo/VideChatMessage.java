package com.videotogether.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideChatMessage {
    private UserVo fromUser;
    private UserVo toUser;

    private Object offer;
    private Object answer;
    private Object candidate;
}
