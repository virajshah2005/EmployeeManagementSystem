package com.operation.ems.models;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private double basicPay;

    private double deductions;

    private LocalDate payDate;

    public Payroll() {}

    public Payroll(Employee employee, double basicPay, double deductions, LocalDate payDate) {
        this.employee = employee;
        this.basicPay = basicPay;
        this.deductions = deductions;
        this.payDate = payDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public double getBasicPay() { return basicPay; }
    public void setBasicPay(double basicPay) { this.basicPay = basicPay; }
    public double getDeductions() { return deductions; }
    public void setDeductions(double deductions) { this.deductions = deductions; }
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
}