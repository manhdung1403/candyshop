package com.finalproject.candyshop.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @GetMapping("/")
    public String rootPage() { return "redirect:/home"; }

    @GetMapping("/home")
    public String homePage() { return "home"; }

    @GetMapping("/cart")
    public String cartPage() { return "cart"; }

    @GetMapping("/orders")
    public String orderHistoryPage() { return "order-history"; }

    @GetMapping("/statistics")
    public String statisticsPage() { return "statistics"; }
}