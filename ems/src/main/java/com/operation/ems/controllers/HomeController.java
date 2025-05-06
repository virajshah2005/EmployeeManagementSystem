package com.operation.ems.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/get_started")
    public String getStarted() {
        return "get_started";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
}