package com.videotogether.exceptionhandle;

import com.videotogether.commen.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

@ControllerAdvice
@RestController
public class AllHandle {

    @ExceptionHandler
    public Result<String> exception(Exception e) {
        return Result.err(e.getMessage());
    }
}
