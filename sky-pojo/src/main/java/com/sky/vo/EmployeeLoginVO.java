package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
//员工登录返回给前端的内容
public class EmployeeLoginVO implements Serializable {
    private Long id;
    private String userName;
    private String name;
    private String token;//JWT令牌
}
