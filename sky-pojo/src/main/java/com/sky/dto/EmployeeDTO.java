package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

//新增员工时接收前端传来的数据
@Data
public class EmployeeDTO implements Serializable {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private String sex;
    private String idNumber;
}
