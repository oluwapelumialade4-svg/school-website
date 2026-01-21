package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.BindingResult;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.service.UserService;

@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("departments", userService.getAllDepartments());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user, BindingResult result, Model model) {
        if (userService.findByUsername(user.getUsername()).isPresent()) {
            result.rejectValue("username", "error.user", "Username is already taken");
        }

        if (user.getAdminCode() != null && !user.getAdminCode().trim().isEmpty()) {
            if ("ADMIN123".equals(user.getAdminCode())) {
                user.setRole(Role.LECTURER);
            } else {
                result.rejectValue("adminCode", "error.user", "Invalid Admin/Lecturer Code");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("departments", userService.getAllDepartments());
            return "register";
        }

        userService.registerUser(user);
        return "redirect:/login?registered";
    }
}