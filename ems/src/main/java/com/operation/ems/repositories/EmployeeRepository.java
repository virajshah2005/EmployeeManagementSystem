package com.operation.ems.repositories;

import com.operation.ems.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Employee findByUserUsername(String username);
    Employee findByEmail(String email);
    long countByUserActive(boolean active);
    long countByCreatedAtAfter(LocalDate date);
    List<Employee> findByStatus(String status);
    List<Employee> findAll(Sort sort);
}