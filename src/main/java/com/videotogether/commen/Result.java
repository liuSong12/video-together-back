package com.videotogether.commen;

import lombok.Data;

@Data
public class Result <T> {
    private Integer code;
    private String msg;
    private T data;

    private Result() {
    }

    private Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private Result(Integer code, String msg,T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data){
        return new Result<>(0,"成功",data);
    }
    public static <T> Result<T> success(String msg, T data){
        return new Result<>(0,msg,data);
    }

    public static <T> Result<T> success(String msg){
        return new Result<>(0,msg);
    }
    public static <T> Result<T> success(){
        return new Result<>(0,"成功");
    }
    public static <T> Result<T> err(String errMsg){
        return new Result<>(2,errMsg);
    }
    public static <T> Result<T> warn(String warnMsg){
        return new Result<>(1,warnMsg);
    }

}
