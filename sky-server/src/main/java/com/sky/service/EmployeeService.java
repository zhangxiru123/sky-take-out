package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    //新增员工
    void save(EmployeeDTO employeeDTO);

    //员工分页查询
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    //启用或禁用员工账号
    void startOrStop(Integer status, Long id);

    //根据id查询员工信息
    Employee getById(Long id);

    //修改员工信息
    void update(EmployeeDTO employeeDTO);
}
