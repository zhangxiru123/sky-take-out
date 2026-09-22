package com.sky.service;

import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;

public interface EmployeeService {
    Employee login(EmployeeLoginDTO employeeLoginDTO);
}
