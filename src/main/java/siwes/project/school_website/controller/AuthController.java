package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, HttpServletRequest request, Model model) {
        try {
            String token = userService.generateResetToken(email);
            String resetUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/reset-password?token=" + token;
            userService.sendResetTokenEmail(email, resetUrl);
            return "redirect:/forgot-password?success";
        } catch (Exception e) {
            return "redirect:/forgot-password?error";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        User user = userService.getByResetPasswordToken(token).orElse(null);
        if (user == null) {
            return "redirect:/login?error=InvalidToken";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token, @RequestParam String password) {
        User user = userService.getByResetPasswordToken(token).orElse(null);
        if (user == null) {
            return "redirect:/login?error=InvalidToken";
        }
        userService.updatePassword(user, password);
        return "redirect:/login?resetSuccess";
    }
}