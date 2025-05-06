package com.operation.ems.controllers;

import com.operation.ems.models.*;
import com.operation.ems.repositories.*;
import com.operation.ems.service.PdfService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeesController {

    private static final Logger logger = LoggerFactory.getLogger(EmployeesController.class);

    @Autowired
    private EmployeeRepository employeeRepo;

    @Autowired
    private LeaveRequestRepository leaveRequestRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private PayrollRepository payrollRepo;

    @Autowired
    private AnnouncementRepository announcementRepo;

    @Autowired
    private PdfService pdfService;

    @GetMapping("/employee-dashboard")
    public String employeeDashboard(Model model, Authentication auth) {
        logger.info("Loading employee dashboard for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        model.addAttribute("employee", employee);
        model.addAttribute("leaveBalance", 20 - leaveRequestRepo.countByEmployeeAndStatus(employee, "Approved"));
        model.addAttribute("announcements", announcementRepo.findTop5ByOrderByCreatedAtDesc());
        return "employee/employee-dashboard";
    }

    @GetMapping("/view-profile")
    public String viewProfile(Model model, Authentication auth) {
        logger.info("Loading view profile for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        model.addAttribute("employee", employee);
        return "employee/view-profile";
    }

    @GetMapping("/edit-profile")
    public String editProfile(Model model, Authentication auth) {
        logger.info("Loading edit profile for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        EmployeeDto employeeDto = new EmployeeDto(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("employeeDto", employeeDto);
        return "employee/edit-profile";
    }

    @PostMapping("/edit-profile")
    public String updateProfile(@Valid @ModelAttribute EmployeeDto employeeDto, BindingResult result, Authentication auth, Model model) {
        logger.info("Updating profile for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        if (result.hasErrors()) {
            logger.warn("Validation errors in edit profile: {}", result.getAllErrors());
            model.addAttribute("employee", employee);
            model.addAttribute("errorMessage", "Please correct the errors in the form.");
            return "employee/edit-profile";
        }
        employee.setPhone(employeeDto.getPhone());
        employee.setAddress(employeeDto.getAddress());
        employeeRepo.save(employee);
        logger.info("Profile updated successfully for employee: {}", employee.getId());
        return "redirect:/employee/view-profile";
    }

    @GetMapping("/apply-leave")
    public String applyLeave(Model model) {
        logger.info("Loading apply leave form");
        model.addAttribute("leaveRequest", new LeaveRequest());
        return "employee/apply-leave";
    }

    @PostMapping("/apply-leave")
    public String submitLeave(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest, BindingResult result, Authentication auth, Model model) {
        logger.info("Processing leave request for user: {}", auth.getName());

        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            model.addAttribute("errorMessage", "Employee not found. Please contact the administrator.");
            return "employee/apply-leave";
        }

        leaveRequest.setEmployee(employee); // Set employee before validation

        // Validate the leave request
        @Valid LeaveRequest validatedLeaveRequest = leaveRequest;
        if (result.hasErrors()) {
            logger.warn("Validation errors in leave request: {}", result.getAllErrors());
            model.addAttribute("errorMessage", "Please correct the errors in the form.");
            return "employee/apply-leave";
        }

        // Validate dates
        LocalDate today = LocalDate.now();
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            logger.warn("Start date or end date is null");
            model.addAttribute("errorMessage", "Start date and end date are required.");
            return "employee/apply-leave";
        }
        if (leaveRequest.getStartDate().isBefore(today) || leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            logger.warn("Invalid date range: startDate={}, endDate={}", leaveRequest.getStartDate(), leaveRequest.getEndDate());
            model.addAttribute("errorMessage", "Start date must be today or later, and end date must be after start date.");
            return "employee/apply-leave";
        }

        leaveRequest.setStatus("Pending");
        leaveRequest.setRequestDate(LocalDate.now());

        try {
            leaveRequestRepo.save(leaveRequest);
            logger.info("Leave request saved successfully for employee: {}", employee.getId());
            model.addAttribute("successMessage", "Leave request submitted successfully.");
            return "redirect:/employee/leave-history";
        } catch (Exception e) {
            logger.error("Failed to save leave request: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to submit leave request. Please try again.");
            return "employee/apply-leave";
        }
    }

    @GetMapping("/leave-history")
    public String leaveHistory(Model model, Authentication auth) {
        logger.info("Loading leave history for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        model.addAttribute("leaveRequests", leaveRequestRepo.findByEmployee(employee));
        return "employee/leave-history";
    }

    @GetMapping("/attendance")
    public String attendance(Model model, Authentication auth) {
        logger.info("Loading attendance for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        model.addAttribute("attendances", attendanceRepo.findByEmployee(employee));
        return "employee/attendance";
    }

    @PostMapping("/clock-in")
    public String clockIn(Authentication auth) {
        logger.info("Processing clock-in for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        Attendance attendance = new Attendance(employee, LocalDate.now(), true);
        attendanceRepo.save(attendance);
        logger.info("Clock-in recorded for employee: {}", employee.getId());
        return "redirect:/employee/attendance";
    }

    @PostMapping("/clock-out")
    public String clockOut(Authentication auth) {
        logger.info("Processing clock-out for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        Attendance attendance = new Attendance(employee, LocalDate.now(), false);
        attendanceRepo.save(attendance);
        logger.info("Clock-out recorded for employee: {}", employee.getId());
        return "redirect:/employee/attendance";
    }

    @GetMapping("/payroll-info")
    public String payrollInfo(Model model, Authentication auth) {
        logger.info("Loading payroll info for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            return "redirect:/login?error";
        }
        Payroll payroll = payrollRepo.findByEmployee(employee);
        if (payroll == null) {
            payroll = new Payroll(employee, 5000, 500, LocalDate.now());
            payrollRepo.save(payroll);
            logger.info("Created default payroll for employee: {}", employee.getId());
        }
        model.addAttribute("payroll", payroll);
        return "employee/payroll-info";
    }

    @GetMapping("/download-payslip")
    public ResponseEntity<InputStreamResource> downloadPayslip(Authentication auth) {
        logger.info("Generating payslip for user: {}", auth.getName());
        Employee employee = employeeRepo.findByUserUsername(auth.getName());
        if (employee == null) {
            logger.error("Employee not found for username: {}", auth.getName());
            throw new IllegalArgumentException("Employee not found");
        }
        Payroll payroll = payrollRepo.findByEmployee(employee);
        if (payroll == null) {
            payroll = new Payroll(employee, 5000, 500, LocalDate.now());
            payrollRepo.save(payroll);
            logger.info("Created default payroll for employee: {}", employee.getId());
        }
        ByteArrayInputStream bis = pdfService.generatePayslip(employee, payroll);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=payslip-" + employee.getId() + ".pdf");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/notifications")
    public String notifications(Model model) {
        logger.info("Loading notifications");
        model.addAttribute("announcements", announcementRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "employee/notifications";
    }
}