package com.arun.jobmailer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        // Forward to the static login page so "/login" works after logout redirects
        return "forward:/login.html";
    }

    @GetMapping("/")
    public String home() {
        // Forward authenticated requests for root to the static index page
        return "forward:/index.html";
    }
}
