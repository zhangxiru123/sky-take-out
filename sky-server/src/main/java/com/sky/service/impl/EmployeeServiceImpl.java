package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired
    private EmployeeMapper employeeMapper;
    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username=employeeLoginDTO.getUsername();
        String password=employeeLoginDTO.getPassword();
       //根据用户名查询数据库中的数据
        Employee employee=employeeMapper.getByUsername(username);
        //处理异常情况
        if(employee==null){
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if(!password.equals(employee.getPassword())){
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus()== StatusConstant.DISABLE) {
            //账户被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        return employee;
    }
}
