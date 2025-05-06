package com.operation.ems.repositories;

import com.operation.ems.models.Employee;
import com.operation.ems.models.LeaveRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    long countByStatus(String status);
    List<LeaveRequest> findAll(Sort sort);
    void deleteByEmployee(Employee employee);
    long countByEmployeeAndStatus(Employee employee, String status);
    List<LeaveRequest> findByEmployee(Employee employee);
}