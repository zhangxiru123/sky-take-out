package com.sky.exception;

//账户找不到异常
public class AccountNotFoundException extends BaseException{
    public AccountNotFoundException() {
    }

    public AccountNotFoundException(String msg) {
        super(msg);
    }
}
