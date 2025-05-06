package com.operation.ems;

import com.operation.ems.models.Announcement;
import com.operation.ems.models.User;
import com.operation.ems.repositories.AnnouncementRepository;
import com.operation.ems.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Date;
import java.time.LocalDate;

@SpringBootApplication
public class EmsApplication implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AnnouncementRepository announcementRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public static void main(String[] args) {
        SpringApplication.run(EmsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setUserType("admin");
            admin.setActive(true);
            userRepository.save(admin);
        }
        if (announcementRepo.count() == 0) {
            Announcement ann = new Announcement();
            ann.setTitle("Welcome to EMS");
            ann.setContent("Explore the new Employee Management System!");
            ann.setCreatedAt(Date.valueOf(LocalDate.now()));
            announcementRepo.save(ann);
        }
    }
}