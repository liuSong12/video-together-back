package com.videotogether.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;
import com.videotogether.config.JwtConfig;
import com.videotogether.pojo.Message;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import lombok.Data;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServerEndpoint("/websocket")
@Component
@Data
public class SoketController {
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
            log.info("【websocket消息】有新的连接，总数为:" + webSockets.size());
        } catch (Exception e) {
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
            log.info("【websocket消息】连接断开，总数为:" + webSockets.size());
        } catch (Exception e) {

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
                    } catch (Exception e) {
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
    public static List<Integer> getRoomUsers(Integer roomId) {
        List<Integer> list = new ArrayList<>();
        for (SoketController webSocket : webSockets) {
            if (webSocket.roomId != null && webSocket.roomId.equals(roomId)) {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    try {
                        list.add(webSocket.userId);
                    } catch (Exception e) {
                    }
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
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    //根据用户id设置房间号
    public static void setRoomIdByUserId(Integer userId, Integer roomId) {
        for (SoketController webSocket : webSockets) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen() && webSocket.userId.equals(userId)) {
                    webSocket.setRoomId(roomId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 此为广播消息所有人都可以接收
     *
     * @param message
     */
    public static void sendAllMessage(String message) {
        log.info("【websocket消息】广播消息:" + message);
        for (SoketController webSocket : webSockets) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 此为单人消息，指定用户id
     *
     * @param userId
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
            } catch (Exception e) {
                e.printStackTrace();
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
