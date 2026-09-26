package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    @Autowired
    private EmployeeMapper employeeMapper;

    //1.用户登录
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
       //密码比对
        //明文密码进行md5加密处理
        password = DigestUtils.md5DigestAsHex(password.getBytes());
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

    //2.新增员工
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee=new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);
        //设置账号状态,默认为1
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码,默认123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));


        employeeMapper.insert(employee);
    }

    //3.员工分页查询
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());

        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        long total = page.getTotal();
        List<Employee> records = page.getResult();
        return new PageResult(total,records);
    }

    //4.启用禁用员工账号
    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();

        employeeMapper.update(employee);
    }

    //5.根据id查询员工信息
    @Override
    public Employee getById(Long id) {
        Employee employee=employeeMapper.getById(id);
        employee.setPassword("****");
        return employee;
    }

    //6.修改员工信息
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee=new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);

        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.update(employee);
    }
}
