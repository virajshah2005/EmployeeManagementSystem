package com.operation.ems.repositories;

import com.operation.ems.models.Employee;
import com.operation.ems.models.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRepository extends JpaRepository<Payroll, Integer> {
    Payroll findByEmployee(Employee employee);
    void deleteByEmployee(Employee employee);
}