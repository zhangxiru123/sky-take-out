package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
//员工登录时前端传递的数据模型
public class EmployeeLoginDTO implements Serializable {
    private String username;
    private String password;
}
