package com.finalproject.candyshop;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    // Đường dẫn truy cập sẽ là http://localhost:8080/welcome
    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to my web application! Dự án đồ án cuối kỳ của chúng em.";
    }
}