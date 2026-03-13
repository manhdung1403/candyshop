package com.finalproject.candyshop.controller;

import com.finalproject.candyshop.model.User;
import com.finalproject.candyshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    // ===== LOGIN =====
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        User user = userService.login(email, password);
        if (user != null) {
            session.setAttribute("loggedUser", user);
            return "redirect:/";
        }
        model.addAttribute("error", "Email hoặc mật khẩu không đúng!");
        return "login";
    }

    // ===== REGISTER =====
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String phone,
                           Model model) {
        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "Email này đã được đăng ký!");
            return "register";
        }
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhone(phone);
        userService.register(user);
        return "redirect:/login?success";
    }

    // ===== LOGOUT =====
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}