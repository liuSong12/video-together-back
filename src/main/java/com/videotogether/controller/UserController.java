package com.videotogether.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.gson.Gson;
import com.videotogether.commen.Result;
import com.videotogether.config.JwtConfig;
import com.videotogether.mapper.UserMapper;
import com.videotogether.pojo.Message;
import com.videotogether.pojo.User;
import com.videotogether.service.impl.UserServiceImpl;
import com.videotogether.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author 裹个小脑
 * @since 2024-01-04
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private SoketController soketController;
    @Autowired
    private Gson gson;

    @PostMapping("/login")
    public Result<String> login(@RequestBody User user, HttpServletResponse response) {
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getPhone, user.getPhone());
        User one = userService.getOne(userQueryWrapper);
        String id;
        if (one == null) {
            userService.save(user);
            User one1 = userService.getOne(userQueryWrapper);
            id = String.valueOf(one1.getId());
        } else {
            id = String.valueOf(one.getId());
        }
        String jwt = JwtConfig.createJWT(id);
        response.addHeader("Authorization", jwt);
        return Result.success("登录成功");
    }

    @GetMapping("/joinRoom")
    public Result<String> joinRoom(Integer roomId, HttpServletRequest request) {
        String infoByToken;
        try {
            infoByToken = JwtConfig.getInfoByToken(request);
            if (infoByToken == null) {
                throw new RuntimeException("请先登录");
            }
        }catch (Exception e){
            throw new RuntimeException("请先登录");
        }
        //加入房间
        SoketController.setRoomIdByUserId(Integer.parseInt(infoByToken), roomId);
        //获取房间内的所有人
        List<Integer> roomUsers = soketController.getRoomUsers(roomId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("users", roomUsers);
        String joinRoomInfo = gson.toJson(map);
        //通知所有人房间列表更新
        soketController.sendRoomAllMessage(roomId, "joinRoom", Integer.valueOf(infoByToken), joinRoomInfo);
        if (roomUsers.size() > 1) {
            new Thread(() -> {
                //这个请求不是第一个，需要获取其他人的观看进度
                List<Integer> list = roomUsers.stream().filter(item -> !item.equals(Integer.valueOf(infoByToken))).toList();
                Integer userId = list.get(0);
                SoketController.sendOneMessage(userId, null, "synchronizeTime", roomId);
            }).start();
        }
        return Result.success("加入房间成功");
    }
}
