package com.sky.exception;

//业务通用异常
public class BaseException extends RuntimeException{
    public BaseException() {
    }

    public BaseException(String msg){
        super(msg);
    }
}
