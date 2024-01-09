package com.videotogether.aop;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.videotogether.config.JwtConfig;
import com.videotogether.pojo.User;
import com.videotogether.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class UserUpdateAOP {
//    @Pointcut("execution(* com.videotogether.controller.*.*(..))")
//    public void update(){}
//
//    @Around("update()")
//    public void updateUserTime(JoinPoint joinPoint){
//
//    }
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private HttpServletRequest request;


    @Around("@annotation(com.videotogether.anno.AnnoUpdateUser)")
    public Object updateUserLastUpdateTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        boolean flag = false;
        for (Object arg : args) {
            if (arg!=null && arg.getClass().equals(User.class)){
                User user = (User) arg;
                if(user.getId()==null) {
                    flag = true;
                    break;
                }
                user.setLastUpdateTime(LocalDateTime.now());
                userService.updateById(user);
                flag = true;
            }
        }
        if(!flag){
            String id = JwtConfig.getInfoByToken(request);
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(User::getLastUpdateTime, LocalDateTime.now()).eq(User::getId, id);
            userService.update(wrapper);
        }
        return joinPoint.proceed();
    }


    @Around("@annotation(com.videotogether.anno.AnnoLastVideoTogether)")
    public Object updateLastVideoTogether(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Boolean flag = false;
        for (Object arg : args) {
            if (arg.getClass().equals(User.class)){
                User user = (User) arg;
                if(user.getId()==null) {
                    flag = true;
                    break;
                }
                user.setLastTogetherWatch(LocalDateTime.now());
                userService.updateById(user);
                flag = true;
            }
        }
        if(!flag){
            String id = JwtConfig.getInfoByToken(request);
            if(id==null){
                return joinPoint.proceed();
            }
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
            wrapper.set(User::getLastTogetherWatch, LocalDateTime.now()).eq(User::getId, id);
            userService.update(wrapper);
        }
        return joinPoint.proceed();
    }
}
