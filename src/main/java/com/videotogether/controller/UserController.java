package com.videotogether.controller;


import ch.qos.logback.core.util.TimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.gson.Gson;
import com.videotogether.anno.AnnoLastVideoTogether;
import com.videotogether.anno.AnnoUpdateUser;
import com.videotogether.commen.Result;
import com.videotogether.config.JwtConfig;
import com.videotogether.pojo.User;
import com.videotogether.pojo.UserVo;
import com.videotogether.service.impl.UserServiceImpl;
import com.videotogether.sms.SendSmsMessage;
import com.videotogether.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final int BUFFER_SIZE = 1024 * 1024;


    @AnnoUpdateUser
    @PostMapping("/login")
    public Result<UserVo> login(@RequestBody User user, HttpServletResponse response) {
        if(user.getCode()==null || user.getPhone()==null){
            throw new RuntimeException("字段缺失");
        }
        // 获取值
        String value = (String) redisTemplate.opsForValue().get(user.getPhone());
        if(value==null){
           throw new RuntimeException("验证码过期");
        }
        if(!user.getCode().equals(value)){
            throw new RuntimeException("验证码错误");
        }
        //清除缓存
        redisTemplate.delete(user.getPhone());
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(User::getPhone, user.getPhone());
        User one = userService.getOne(userQueryWrapper);
        String id;
        if (one == null) {
            userService.save(user);
            one = userService.getOne(userQueryWrapper);
            id = String.valueOf(one.getId());
        } else {
            id = String.valueOf(one.getId());
        }
        String jwt = JwtConfig.createJWT(id);
        response.addHeader("Authorization", jwt);
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(one, userVo);
        return Result.success(userVo);
    }

    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody User user) {
        if(user.getPhone()==null){
            throw new RuntimeException("字段缺失");
        }
        Object code = redisTemplate.opsForValue().get(user.getPhone());
        if (code != null) {
            throw new RuntimeException("验证码已经发送");
        }else {
            code = String.valueOf(Utils.generateValidateCode(4));
            redisTemplate.opsForValue().set(user.getPhone(), code, 1,TimeUnit.MINUTES);
            SendSmsMessage.send(user.getPhone(), (String) code);
            return Result.success("ok");
        }
    }

    @AnnoUpdateUser
    @GetMapping("/leaveRoom")
    public Result<String> leaveRoom(Integer roomId, HttpServletRequest request) {
        String infoByToken;
        try {
            infoByToken = JwtConfig.getInfoByToken(request);
            if (infoByToken == null) {
                throw new RuntimeException("请先登录");
            }
        } catch (Exception e) {
            throw new RuntimeException("请先登录");
        }
        //离开房间
        SoketController.setRoomIdByUserId(Integer.parseInt(infoByToken), null);
        //获取房间内的所有人
        List<UserVo> roomUsers = soketController.getRoomUsers(roomId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("users", roomUsers);
        String joinRoomInfo = gson.toJson(map);
        //通知所有人房间列表更新
        soketController.sendRoomAllMessage(roomId, "joinRoom", Integer.valueOf(infoByToken), joinRoomInfo);
        return Result.success("ok");
    }

    @AnnoUpdateUser
    @GetMapping("/joinRoom")
    public Result<String> joinRoom(Integer roomId, HttpServletRequest request) {
        String infoByToken;
        try {
            infoByToken = JwtConfig.getInfoByToken(request);
            if (infoByToken == null) {
                throw new RuntimeException("请先登录");
            }
        } catch (Exception e) {
            throw new RuntimeException("请先登录");
        }
        //加入房间
        SoketController.setRoomIdByUserId(Integer.parseInt(infoByToken), roomId);
        //获取房间内的所有人
        List<UserVo> roomUsers = soketController.getRoomUsers(roomId);
        HashMap<String, Object> map = new HashMap<>();
        map.put("users", roomUsers);
        String joinRoomInfo = gson.toJson(map);
        //通知所有人房间列表更新
        soketController.sendRoomAllMessage(roomId, "joinRoom", Integer.valueOf(infoByToken), joinRoomInfo);
        if (roomUsers.size() > 1) {
            new Thread(() -> {
                //这个请求不是第一个，需要获取其他人的观看进度
                List<UserVo> list = roomUsers.stream().filter(item -> item.getId() != (Integer.valueOf(infoByToken))).toList();
                UserVo user = list.get(0);
                SoketController.sendOneMessage(user.getId(), null, "synchronizeTime", roomId);
            }).start();
        }
        return Result.success("加入房间成功");
    }

    @AnnoUpdateUser
    @GetMapping("/getUser")
    public Result<UserVo> getUser(HttpServletRequest request) {
        String uId = JwtConfig.getInfoByToken(request);
        User user = userService.getById(Integer.valueOf(uId));
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return Result.success(userVo);
    }

    @AnnoUpdateUser
    @PostMapping("/update")
    public Result<UserVo> update(@RequestParam(required = false) String userName,
                                 @RequestParam(required = false) String avatarHash,
                                 @RequestParam(required = false) String avatarExtenSion,
                                 @RequestParam(required = false) String avatarName,
                                 @RequestParam(required = false) MultipartFile avatar,
                                 HttpServletRequest request
    ) throws Exception {
        if (userName == null || avatar == null) {
            throw new RuntimeException("userName和avatar不能为null");
        }
        if (avatar != null) {
            if (avatarHash == null) {
                throw new RuntimeException("avatarHash为null");
            }
        }
        File file = new File("avatar");
        if (!file.exists()) {
            file.mkdirs();
        }
        File avatarPath = new File(file, avatarHash + avatarExtenSion);
        if (!avatarPath.exists()) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(avatar.getInputStream());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(avatarPath));
            byte[] bytes = new byte[BUFFER_SIZE];
            int len;
            while ((len = bufferedInputStream.read(bytes)) != -1) {
                bufferedOutputStream.write(bytes, 0, len);
            }
        }
        String uId = JwtConfig.getInfoByToken(request);
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(User::getUserName, userName).set(User::getAvatar, avatarPath.getName()).eq(User::getId, Integer.valueOf(uId));
        userService.update(wrapper);
        UserVo userVo = new UserVo();
        userVo.setUserName(userName);
        userVo.setAvatar(avatarPath.getName());
        userVo.setId(Integer.valueOf(uId));
        return Result.success(userVo);
    }


    /**
     * 这个是头像
     * @param avatarPath
     * @param response
     * @throws Exception
     */
    @GetMapping("/{avatarPath}")
    public void getAvatar(@PathVariable("avatarPath") String avatarPath, HttpServletResponse response) throws Exception {
        File file = new File("avatar");
        if (!file.exists()) {
            file.mkdirs();
        }
        File avatar = new File(file, avatarPath);
        if (!avatar.exists()) {
            throw new RuntimeException("文件不存在");
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(avatar));
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
        byte[] bytes = new byte[BUFFER_SIZE];
        int len;
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, len);
        }
    }

    @AnnoUpdateUser
    @GetMapping("/allUser")
    public Result<List<UserVo>> getAllActiveUser() {
        List<UserVo> allUsers = SoketController.getAllUsers();
        return Result.success(allUsers);
    }

    @AnnoUpdateUser
    @AnnoLastVideoTogether
    @GetMapping("/watchTogether")
    public Result<String> watchTogether(){
        return Result.success("成功");
    }

}
