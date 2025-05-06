package com.operation.ems.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate date;

    private boolean isClockIn;

    public Attendance() {}

    public Attendance(Employee employee, LocalDate date, boolean isClockIn) {
        this.employee = employee;
        this.date = date;
        this.isClockIn = isClockIn;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isClockIn() { return isClockIn; }
    public void setClockIn(boolean clockIn) { isClockIn = clockIn; }
}