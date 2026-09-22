package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

//全局异常处理器
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //捕获业务异常
    @ExceptionHandler(BaseException.class)
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息:{}",ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result exceptionHandler(SQLIntegrityConstraintViolationException ex){
        String message = ex.getMessage();
        //检测异常是否由重复引起
        if(message.contains("Duplicate entry")){
            //截取重复字段
            String[] split = message.split(" ");
            String username = split[2];
            String msg = username+ MessageConstant.ALREADY_EXISTS;
            return  Result.error(msg);
        }else{
            //不是由重复引起,未知异常
            return Result.error(MessageConstant.UNKNOWN_ERROR);
        }
    }
}
