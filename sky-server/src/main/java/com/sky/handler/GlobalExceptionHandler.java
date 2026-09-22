package com.sky.handler;

import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
//全局异常处理器
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息:{}",ex.getMessage());
        return Result.error(ex.getMessage());
    }
}
