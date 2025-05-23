package com.operation.ems.repositories;

import com.operation.ems.models.Attendance;
import com.operation.ems.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    List<Attendance> findByEmployee(Employee employee);
}