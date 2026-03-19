package com.finalproject.candyshop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/")
    public String rootPage() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String homePage() { return "home"; }
}