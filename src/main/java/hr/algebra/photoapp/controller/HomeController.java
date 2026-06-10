package hr.algebra.photoapp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Controller Pattern (MVC)
// Handles home page requests
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        // Redirect authenticated users to photos, show landing page for anonymous
        if (authentication != null && authentication.isAuthenticated() 
                && !authentication.getName().equals("anonymousUser")) {
            return "redirect:/photos";
        }
        return "index";
    }
}
