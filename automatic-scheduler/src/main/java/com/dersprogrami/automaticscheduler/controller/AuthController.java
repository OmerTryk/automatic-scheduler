package com.dersprogrami.automaticscheduler.controller;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.service.DepartmentService;
import com.dersprogrami.automaticscheduler.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;
    private final DepartmentService departmentService;

    public AuthController(UserService userService, DepartmentService departmentService) {
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping("/login")
    public String login(Model model, @RequestParam(required = false) String error) {
    
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        
        if (error != null) {
            model.addAttribute("error", "Kullanıcı adı veya şifre hatalı!");
        }
        
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
       
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        
        model.addAttribute("user", new User());
        model.addAttribute("departments", departmentService.getAllDepartments());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") @Valid User user, 
                              BindingResult result, 
                              @RequestParam("role") String role,
                              @RequestParam(value = "departmentId", required = false) Long departmentId,
                              Model model) {
        
        if (userService.isUsernameExists(user.getUsername())) {
            result.rejectValue("username", "error.user", "Bu kullanıcı adı zaten kullanılıyor!");
        }
        
        if (userService.isEmailExists(user.getEmail())) {
            result.rejectValue("email", "error.user", "Bu e-posta adresi zaten kullanılıyor!");
        }
        
        if (result.hasErrors()) {
            return "register";
        }
        
        if (!role.equals("ROLE_INSTRUCTOR") && !role.equals("ROLE_DEPARTMENT_HEAD")) {
            model.addAttribute("roleError", "Geçersiz rol seçimi!");
            return "register";
        }
        
        // Bölüm seçimi kontrolü
        if (departmentId == null) {
            model.addAttribute("departmentError", "Lütfen bir bölüm seçin!");
            model.addAttribute("departments", departmentService.getAllDepartments());
            return "register";
        }
        
        Department department = departmentService.getDepartmentById(departmentId)
            .orElse(null);
            
        if (department == null) {
            model.addAttribute("departmentError", "Geçersiz bölüm seçimi!");
            model.addAttribute("departments", departmentService.getAllDepartments());
            return "register";
        }
        
        userService.registerUser(user, role, department);
        
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest request) {
      
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEPARTMENT_HEAD"))) {
            return "redirect:/department-head/dashboard";
        } else if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"))) {
            return "redirect:/instructor/dashboard";
        }
        
        return "redirect:/";
    }

    @GetMapping("/")
    public String home() {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        
        return "index";
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
              !(authentication instanceof AnonymousAuthenticationToken) && 
              authentication.isAuthenticated();
    }
}