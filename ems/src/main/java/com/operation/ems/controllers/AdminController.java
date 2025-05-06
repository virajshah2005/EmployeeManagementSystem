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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private EmployeeRepository employeeRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private LeaveRequestRepository leaveRequestRepo;

    @Autowired
    private PayrollRepository payrollRepo;

    @Autowired
    private AnnouncementRepository announcementRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private PdfService pdfService;

    @GetMapping("/admin-dashboard")
    public String adminDashboard(Model model) {
        logger.info("Loading admin dashboard");
        model.addAttribute("totalEmployees", employeeRepo.count());
        model.addAttribute("activeEmployees", employeeRepo.countByUserActive(true));
        model.addAttribute("pendingLeaves", leaveRequestRepo.countByStatus("Pending"));
        model.addAttribute("newJoiners", employeeRepo.countByCreatedAtAfter(LocalDate.now().minusDays(30)));
        return "admin/admin-dashboard";
    }

    @GetMapping("/employee-list")
    public String getEmployees(Model model, @RequestParam(required = false) String role) {
        logger.info("Loading employee list with role filter: {}", role);
        List<Employee> employees = role != null && !role.isEmpty() ?
            employeeRepo.findByStatus(role) : employeeRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("employees", employees);
        model.addAttribute("role", role);
        return "admin/employee-list";
    }

    @GetMapping("/add-employee")
    public String addEmployee(Model model) {
        logger.info("Loading add employee form");
        model.addAttribute("employeeDto", new EmployeeDto());
        return "admin/add-employee";
    }

    @PostMapping("/add-employee")
    public String createEmployee(@Valid @ModelAttribute EmployeeDto employeeDto, BindingResult result,
                                 @RequestParam("document") MultipartFile document, Model model) {
        logger.info("Creating new employee with username: {}", employeeDto.getUsername());
        if (employeeRepo.findByEmail(employeeDto.getEmail()) != null) {
            result.addError(new FieldError("employeeDto", "email", "Email address is already used"));
        }
        if (userRepo.findByUsername(employeeDto.getUsername()) != null) {
            result.addError(new FieldError("employeeDto", "username", "Username is already taken"));
        }
        if (result.hasErrors()) {
            logger.warn("Validation errors in add employee: {}", result.getAllErrors());
            return "admin/add-employee";
        }

        // Create and save User entity first
        User user = new User();
        user.setUsername(employeeDto.getUsername());
        user.setPassword(passwordEncoder.encode(employeeDto.getPassword()));
        user.setUserType("employee");
        user.setActive(!employeeDto.getStatus().equals("Inactive"));
        userRepo.save(user); // Save User to generate ID

        // Create Employee entity
        Employee employee = new Employee();
        employee.setFirstName(employeeDto.getFirstName());
        employee.setLastName(employeeDto.getLastName());
        employee.setEmail(employeeDto.getEmail());
        employee.setPhone(employeeDto.getPhone());
        employee.setAddress(employeeDto.getAddress());
        employee.setStatus(employeeDto.getStatus());
        employee.setCreatedAt(LocalDate.now());
        try {
            if (!document.isEmpty()) {
                employee.setDocument(document.getBytes());
                employee.setDocumentName(document.getOriginalFilename());
            }
        } catch (IOException e) {
            logger.error("Failed to upload document: {}", e.getMessage());
            result.addError(new FieldError("employeeDto", "document", "Failed to upload document"));
            return "admin/add-employee";
        }
        employee.setUser(user); // Associate the saved User
        employeeRepo.save(employee);
        payrollRepo.save(new Payroll(employee, 5000, 500, LocalDate.now()));
        logger.info("Employee created successfully: {}", employee.getId());
        return "redirect:/admin/employee-list";
    }

    @GetMapping("/edit-employee")
    public String editEmployee(Model model, @RequestParam int id) {
        logger.info("Loading edit employee form for ID: {}", id);
        Employee employee = employeeRepo.findById(id).orElseThrow(() -> {
            logger.error("Employee not found: {}", id);
            return new IllegalArgumentException("Invalid employee ID");
        });
        EmployeeDto employeeDto = new EmployeeDto(employee);
        model.addAttribute("employee", employee);
        model.addAttribute("employeeDto", employeeDto);
        return "admin/edit-employee";
    }

    @PostMapping("/edit-employee")
    public String updateEmployee(@RequestParam int id, @Valid @ModelAttribute EmployeeDto employeeDto,
                                 BindingResult result, @RequestParam("document") MultipartFile document, Model model) {
        logger.info("Updating employee ID: {}", id);
        Employee employee = employeeRepo.findById(id).orElseThrow(() -> {
            logger.error("Employee not found: {}", id);
            return new IllegalArgumentException("Invalid employee ID");
        });
        if (!employee.getEmail().equals(employeeDto.getEmail()) && employeeRepo.findByEmail(employeeDto.getEmail()) != null) {
            result.addError(new FieldError("employeeDto", "email", "Email address is already used"));
        }
        if (result.hasErrors()) {
            logger.warn("Validation errors in edit employee: {}", result.getAllErrors());
            model.addAttribute("employee", employee);
            return "admin/edit-employee";
        }

        employee.setFirstName(employeeDto.getFirstName());
        employee.setLastName(employeeDto.getLastName());
        employee.setEmail(employeeDto.getEmail());
        employee.setPhone(employeeDto.getPhone());
        employee.setAddress(employeeDto.getAddress());
        employee.setStatus(employeeDto.getStatus());
        employee.getUser().setActive(!employeeDto.getStatus().equals("Inactive"));
        if (!employeeDto.getPassword().isEmpty()) {
            employee.getUser().setPassword(passwordEncoder.encode(employeeDto.getPassword()));
        }
        try {
            if (!document.isEmpty()) {
                employee.setDocument(document.getBytes());
                employee.setDocumentName(document.getOriginalFilename());
            }
        } catch (IOException e) {
            logger.error("Failed to upload document: {}", e.getMessage());
            result.addError(new FieldError("employeeDto", "document", "Failed to upload document"));
            return "admin/edit-employee";
        }
        employeeRepo.save(employee);
        logger.info("Employee updated successfully: {}", employee.getId());
        return "redirect:/admin/employee-list";
    }

	/*
	 * @GetMapping("/employee-profile") public String viewProfile(@RequestParam int
	 * id, Model model) { logger.info("Loading employee profile for ID: {}", id);
	 * Employee employee = employeeRepo.findById(id).orElseThrow(() -> {
	 * logger.error("Employee not found: {}", id); return new
	 * IllegalArgumentException("Invalid employee ID"); });
	 * model.addAttribute("employee", employee); return "admin/employee-profile"; }
	 */
    @GetMapping("/employee-profile")
    public String employeeProfile(@RequestParam(value = "id", required = false) Integer employeeId, Model model, RedirectAttributes redirectAttributes) {
        logger.info("Loading employee profile for employee ID: {}", employeeId);

        // Check if the ID parameter is provided
        if (employeeId == null) {
            logger.warn("Missing required parameter 'id' for employee profile");
            redirectAttributes.addFlashAttribute("errorMessage", "Employee ID is required to view the profile.");
            return "redirect:/admin/employee-list";
        }

        // Retrieve the employee by ID
        Employee employee = employeeRepo.findById(employeeId).orElse(null);
        if (employee == null) {
            logger.error("Employee not found for ID: {}", employeeId);
            redirectAttributes.addFlashAttribute("errorMessage", "Employee not found.");
            return "redirect:/admin/employee-list";
        }

        model.addAttribute("employee", employee);
        return "admin/employee-profile";
    }

    @Transactional
    @GetMapping("/delete-employee")
    public String deleteEmployee(@RequestParam int id) {
        logger.info("Deleting employee ID: {}", id);
        Employee employee = employeeRepo.findById(id).orElseThrow(() -> {
            logger.error("Employee not found: {}", id);
            return new IllegalArgumentException("Invalid employee ID");
        });

        // Delete dependent records
        payrollRepo.deleteByEmployee(employee);
        leaveRequestRepo.deleteByEmployee(employee);

        // Delete the Employee and associated User
        User user = employee.getUser();
        employeeRepo.delete(employee);
        if (user != null) {
            userRepo.delete(user);
            logger.info("Associated user deleted: {}", user.getId());
        }

        logger.info("Employee and related records deleted successfully: {}", id);
        return "redirect:/admin/employee-list";
    }

    @GetMapping("/leave-requests")
    public String leaveRequests(Model model) {
        logger.info("Loading leave requests for admin");
        model.addAttribute("leaveRequests", leaveRequestRepo.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "admin/leave-requests";
    }

    @PostMapping("/update-leave-status")
    public String updateLeaveStatus(@RequestParam int id, @RequestParam String status) {
        logger.info("Updating leave request ID: {} to status: {}", id, status);
        LeaveRequest leave = leaveRequestRepo.findById(id).orElseThrow(() -> {
            logger.error("Leave request not found: {}", id);
            return new IllegalArgumentException("Invalid leave ID");
        });
        leave.setStatus(status);
        leaveRequestRepo.save(leave);
        logger.info("Leave request ID: {} updated to {}", id, status);
        return "redirect:/admin/leave-requests";
    }

    @GetMapping("/apply-leave-admin")
    public String applyLeaveAdmin(Model model) {
        logger.info("Loading apply leave form for admin");
        model.addAttribute("leaveRequest", new LeaveRequest());
        model.addAttribute("employees", employeeRepo.findAll());
        return "admin/apply-leave-admin";
    }

    @PostMapping("/apply-leave-admin")
    public String submitLeaveAdmin(@ModelAttribute("leaveRequest") LeaveRequest leaveRequest, BindingResult result, Model model) {
        logger.info("Processing admin leave request submission");

        if (leaveRequest.getEmployee() == null) {
            logger.warn("Employee is null in admin leave request");
            model.addAttribute("employees", employeeRepo.findAll());
            model.addAttribute("errorMessage", "Employee is required.");
            return "admin/apply-leave-admin";
        }

        // Validate the leave request
        @Valid LeaveRequest validatedLeaveRequest = leaveRequest;
        if (result.hasErrors()) {
            logger.warn("Validation errors in admin leave request: {}", result.getAllErrors());
            model.addAttribute("employees", employeeRepo.findAll());
            model.addAttribute("errorMessage", "Please correct the errors in the form.");
            return "admin/apply-leave-admin";
        }

        // Validate dates
        LocalDate today = LocalDate.now();
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            logger.warn("Start date or end date is null");
            model.addAttribute("employees", employeeRepo.findAll());
            model.addAttribute("errorMessage", "Start date and end date are required.");
            return "admin/apply-leave-admin";
        }
        if (leaveRequest.getStartDate().isBefore(today) || leaveRequest.getEndDate().isBefore(leaveRequest.getStartDate())) {
            logger.warn("Invalid date range: startDate={}, endDate={}", leaveRequest.getStartDate(), leaveRequest.getEndDate());
            model.addAttribute("employees", employeeRepo.findAll());
            model.addAttribute("errorMessage", "Start date must be today or later, and end date must be after start date.");
            return "admin/apply-leave-admin";
        }

        leaveRequest.setStatus("Pending");
        leaveRequest.setRequestDate(LocalDate.now());

        try {
            leaveRequestRepo.save(leaveRequest);
            logger.info("Admin leave request saved successfully");
            return "redirect:/admin/leave-requests";
        } catch (Exception e) {
            logger.error("Failed to save admin leave request: {}", e.getMessage(), e);
            model.addAttribute("employees", employeeRepo.findAll());
            model.addAttribute("errorMessage", "Failed to submit leave request. Please try again.");
            return "admin/apply-leave-admin";
        }
    }

    @GetMapping("/salary-dashboard")
    public String salaryDashboard(Model model) {
        logger.info("Loading salary dashboard");
        model.addAttribute("payrolls", payrollRepo.findAll());
        return "admin/salary-dashboard";
    }

    @GetMapping("/generate-payslips")
    public String generatePayslips(Model model) {
        logger.info("Loading generate payslips page");
        model.addAttribute("employees", employeeRepo.findAll());
        return "admin/generate-payslips";
    }

    @GetMapping("/download-payslip")
    public ResponseEntity<InputStreamResource> downloadPayslip(@RequestParam int employeeId) {
        logger.info("Generating payslip for employee ID: {}", employeeId);
        Employee employee = employeeRepo.findById(employeeId).orElseThrow(() -> {
            logger.error("Employee not found: {}", employeeId);
            return new IllegalArgumentException("Invalid employee ID");
        });
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

    @GetMapping("/reports-analytics")
    public String reportsAnalytics(Model model) {
        logger.info("Loading reports and analytics");
        model.addAttribute("totalEmployees", employeeRepo.count());
        model.addAttribute("leaveCount", leaveRequestRepo.count());
        return "admin/reports-analytics";
    }

    @PostMapping("/toggle-status")
    public String toggleStatus(@RequestParam int id) {
        logger.info("Toggling status for employee ID: {}", id);
        Employee employee = employeeRepo.findById(id).orElse(null);
        if (employee != null) {
            if (employee.getStatus().equals("Inactive")) {
                employee.setStatus("Permanent");
                employee.getUser().setActive(true);
            } else {
                employee.setStatus("Inactive");
                employee.getUser().setActive(false);
            }
            employeeRepo.save(employee);
            logger.info("Employee status toggled: {}", employee.getId());
        }
        return "redirect:/admin/employee-list";
    }
}