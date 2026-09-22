package com.sky.result;


import lombok.Data;

/**
 * 统一返回结果
 @param <T>
*/

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;

    public static <T> Result<T> success(){
        Result<T> result=new Result<T>();
        result.code=1;
        return result;
    }

    public static <T> Result<T> success(T object){
        Result<T> result=new Result<T>();
        result.data=object;
        result.code=1;
        return result;
    }

    public static <T> Result<T> error(String msg){
        Result<T> result=new Result<T>();
        result.code=0;
        result.msg=msg;
        return result;
    }
}
