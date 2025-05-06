package com.operation.ems.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.operation.ems.models.Employee;
import com.operation.ems.models.Payroll;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public ByteArrayInputStream generatePayslip(Employee employee, Payroll payroll) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        document.add(new Paragraph("Payslip for " + employee.getFirstName() + " " + employee.getLastName()));
        document.add(new Paragraph("Employee ID: " + employee.getId()));
        document.add(new Paragraph("Basic Pay: $" + payroll.getBasicPay()));
        document.add(new Paragraph("Deductions: $" + payroll.getDeductions()));
        document.add(new Paragraph("Net Pay: $" + (payroll.getBasicPay() - payroll.getDeductions())));
        document.add(new Paragraph("Date: " + payroll.getPayDate()));

        document.close();
        return new ByteArrayInputStream(out.toByteArray());
    }
}