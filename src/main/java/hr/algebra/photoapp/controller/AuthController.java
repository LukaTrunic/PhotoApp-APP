package hr.algebra.photoapp.controller;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Controller Pattern (MVC)
// Handles authentication-related requests (login, registration)
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final UserActionService userActionService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("packages", PackageType.values());
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam PackageType packageType,
                          RedirectAttributes redirectAttributes,
                          HttpServletRequest request) {

        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Username is required");
            return "redirect:/register";
        }

        if (email == null || email.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email is required");
            return "redirect:/register";
        }

        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters");
            return "redirect:/register";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match");
            return "redirect:/register";
        }

        try {
            userService.registerUser(username, email, password, packageType);
            
            // Log registration action
            userActionService.logAction(username, "REGISTER", 
                    "User registered with package: " + packageType);
            
            redirectAttributes.addFlashAttribute("success", 
                    "Registration successful! Please log in.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }
}
