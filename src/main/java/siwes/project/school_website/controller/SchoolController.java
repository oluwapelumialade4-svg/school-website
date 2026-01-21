package siwes.project.school_website.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SchoolController {

    @GetMapping("/api")
    public String home() {
        return "Welcome to the School Website API!";
    }
}
