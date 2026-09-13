package com.projectguard.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @GetMapping("/profile")
    public String profile(Authentication authentication) {

        return "Student profile API accessed by: "
                + authentication.getName();
    }
}