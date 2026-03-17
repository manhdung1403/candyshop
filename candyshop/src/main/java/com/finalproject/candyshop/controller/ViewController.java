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
    public String homePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        model.addAttribute("isLoggedIn", username != null);
        model.addAttribute("username", username);
        return "home";
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);
        return "profile";
    }

    @GetMapping("/editprofile")
    public String editProfilePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);
        return "editprofile";
    }
};