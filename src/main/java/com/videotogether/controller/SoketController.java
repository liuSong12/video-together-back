package com.videotogether.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;
import com.videotogether.config.JwtConfig;
import com.videotogether.pojo.*;
import com.videotogether.service.impl.UserServiceImpl;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServerEndpoint("/websocket")
@Component
public class SoketController {

    private static UserServiceImpl userService;

    @Autowired
    public void setUserService(UserServiceImpl userService) {
        SoketController.userService = userService;
    }

    private static final Gson gson = new Gson();
    //与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;
    /**
     * 用户ID
     */
    private Integer userId;
    private Integer roomId;

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
    //虽然@Component默认是单例模式的，但springboot还是会为每个websocket连接初始化一个bean，所以可以用一个静态set保存起来。
    //  注：底下WebSocket是当前类名
    public static CopyOnWriteArraySet<SoketController> webSockets = new CopyOnWriteArraySet<>();

    // 用来存在线连接用户信息
    public static ConcurrentHashMap<Integer, Session> sessionPool = new ConcurrentHashMap<>();

    /**
     * 链接成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        try {
            String queryString = session.getQueryString();
            if (queryString == null) {
                return;
            }
            String token = queryString.split("=")[1];
            String userId;
            try {
                userId = JwtConfig.getSubject(token);
            } catch (Exception e) {
                return;
            }
            this.userId = Integer.valueOf(userId);
            this.session = session;
            webSockets.add(this);
            sessionPool.put(Integer.valueOf(userId), session);
            List<UserVo> allUsers = getAllUsers();
            String json = gson.toJson(allUsers);
            Message message = new Message(null, json,null, "online", null);
            sendAllMessage(gson.toJson(message));
        } catch (Exception ignored) {
        }
    }

    /**
     * 链接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            List<UserVo> allUsers = getAllUsers();
            String json = gson.toJson(allUsers);
            Message message = new Message(null, json,null, "online", null);
            sendAllMessage(gson.toJson(message));
        } catch (Exception ignored) {

        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     */
    @OnMessage
    public void onMessage(String message) {
        Message clientMessage = gson.fromJson(message, Message.class);
        int uId;
        try {
            uId = Integer.parseInt(JwtConfig.getSubject(clientMessage.getToken()));
        } catch (Exception e) {
            return;
        }
        Integer roomId = clientMessage.getRoomId();
        Object msg = clientMessage.getMessage();
        String type = clientMessage.getType();
        switch (type) {
            case "play":
            case "pause":
            case "timeupdate":
            case "synchronizeTimeAsRoomHost":
                sendRoomMessage(roomId, type, uId, msg);
                break;
            case "chat":
                ChatMsg chatMsg = gson.fromJson(msg.toString(), ChatMsg.class);
                Integer from_uId = chatMsg.getFrom();
                if (from_uId == null) {
                    return;
                }
                User user = userService.getById(from_uId);
                chatMsg.setAvatar(user.getAvatar());
                chatMsg.setUserName(user.getUserName());
                sendRoomMessage(roomId, type, uId, chatMsg);
                break;
            case "call":
                UserVo toId = gson.fromJson(msg.toString(), UserVo.class);
                Integer id = toId.getId();

                User fromUser = userService.getById(uId);
                UserVo fromUserVo = new UserVo();
                BeanUtils.copyProperties(fromUser, fromUserVo);

                User toUser = userService.getById(id);
                UserVo toUserVo = new UserVo();
                BeanUtils.copyProperties(toUser, toUserVo);

                VideChatMessage videChatMessage = new VideChatMessage(fromUserVo, toUserVo);
                String json = gson.toJson(videChatMessage);
                sendOneMessage(id, json, type, roomId);
                break;
            default:
                break;
        }

    }

    /**
     * 发送错误时的处理
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误,原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 此为房间消息，把信息发送给房间内的所有用户
     *
     * @param roomId
     * @param type
     * @param userId
     * @param message
     */
    public void sendRoomAllMessage(Integer roomId, String type, Integer userId, String message) {
        for (SoketController webSocket : webSockets) {
            if (webSocket.roomId != null && webSocket.roomId.equals(roomId)) {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    try {
                        String json = gson.toJson(new Message(userId, message, roomId, type, null));
                        webSocket.session.getAsyncRemote().sendText(json);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /**
     * 获取房间内所有用户
     *
     * @param roomId
     */
    public static List<UserVo> getRoomUsers(Integer roomId) {
        List<UserVo> list = new ArrayList<>();
        for (SoketController webSocket : webSockets) {
            if (webSocket.roomId != null && webSocket.roomId.equals(roomId)) {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    try {
                        User byId = userService.getById(webSocket.userId);
                        UserVo userVo = new UserVo();
                        BeanUtils.copyProperties(byId, userVo);
                        list.add(userVo);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return list;
    }

    /**
     * 获取所有用户
     *
     * @return
     */
    public static List<UserVo> getAllUsers() {
        List<UserVo> list = new ArrayList<>();
        for (SoketController webSocket : webSockets) {
            if (webSocket.session != null && webSocket.session.isOpen()) {
                try {
                    User byId = userService.getById(webSocket.userId);
                    UserVo userVo = new UserVo();
                    BeanUtils.copyProperties(byId, userVo);
                    list.add(userVo);
                } catch (Exception ignored) {
                }
            }
        }
        return list;
    }


    //此为房间消息，把信息发送给房间内的其他用户
    public void sendRoomMessage(Integer roomId, String type, Integer userId1, Object message) {
        for (SoketController webSocket : webSockets) {
            if (webSocket.roomId != null && webSocket.roomId.equals(roomId) && !webSocket.userId.equals(userId1)) {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    try {
                        String json = gson.toJson(new Message(userId1, message, roomId, type, null));
                        webSocket.session.getAsyncRemote().sendText(json);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }

    /**
     * 根据用户id设置房间号
     * @param userId
     * @param roomId
     */
    public static void setRoomIdByUserId(Integer userId, Integer roomId) {
        for (SoketController webSocket : webSockets) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen() && webSocket.userId.equals(userId)) {
                    webSocket.setRoomId(roomId);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 根据用户id获取房间号
     *
     * @param userId
     * @return
     */
    public static Integer getRoomIdByUserId(Integer userId) {
        for (SoketController webSocket : webSockets) {
            if (webSocket.session != null && webSocket.session.isOpen() && webSocket.userId.equals(userId)) {
                return webSocket.roomId;
            }
        }
        return null;
    }




    /**
     * 此为广播消息所有人都可以接收
     *
     * @param message
     */
    public static void sendAllMessage(String message) {
        for (SoketController webSocket : webSockets) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 此为单人消息，指定用户id
     *
     * @param userId 发给谁
     * @param message
     * @param type
     * @param roomId
     */
    public static void sendOneMessage(Integer userId, String message, String type, Integer roomId) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = gson.toJson(new Message(userId, message, roomId, type, null));
                session.getAsyncRemote().sendText(json);
            } catch (Exception ignored) {
            }
        }
    }

    // 此为单点消息(多人)
    public void sendMoreMessage(String[] userIds, String message) {
        for (String userId : userIds) {
            Session session = sessionPool.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.getAsyncRemote().sendText(message);
                } catch (Exception ignored) {
                }
            }
        }

    }

}
