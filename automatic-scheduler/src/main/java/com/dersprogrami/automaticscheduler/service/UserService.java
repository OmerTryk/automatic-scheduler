package com.dersprogrami.automaticscheduler.service;

import com.dersprogrami.automaticscheduler.model.Department;
import com.dersprogrami.automaticscheduler.model.DepartmentHead;
import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.model.Role;
import com.dersprogrami.automaticscheduler.model.User;
import com.dersprogrami.automaticscheduler.repository.DepartmentHeadRepository;
import com.dersprogrami.automaticscheduler.repository.InstructorRepository;
import com.dersprogrami.automaticscheduler.repository.RoleRepository;
import com.dersprogrami.automaticscheduler.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final InstructorRepository instructorRepository;
    private final DepartmentHeadRepository departmentHeadRepository;

    public UserService(UserRepository userRepository, 
                       RoleRepository roleRepository, 
                       PasswordEncoder passwordEncoder,
                       InstructorRepository instructorRepository,
                       DepartmentHeadRepository departmentHeadRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.instructorRepository = instructorRepository;
        this.departmentHeadRepository = departmentHeadRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + username));
    }

    @Transactional
    public User registerUser(User user, String roleName, Department department) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName(roleName)
                .ifPresent(roles::add);
        user.setRoles(roles);
        
        User savedUser = userRepository.save(user);
        
        // Bölüm ataması yapılır
        if (roleName.equals("ROLE_INSTRUCTOR")) {
            Instructor instructor = new Instructor();
            instructor.setUser(savedUser);
            instructor.setDepartment(department);
            instructorRepository.save(instructor);
        } else if (roleName.equals("ROLE_DEPARTMENT_HEAD")) {
            DepartmentHead departmentHead = new DepartmentHead();
            departmentHead.setUser(savedUser);
            departmentHead.setDepartment(department);
            departmentHeadRepository.save(departmentHead);
        }
        
        return savedUser;
    }

    @Transactional
    public User updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            userRepository.findById(user.getId())
                    .ifPresent(existingUser -> user.setPassword(existingUser.getPassword()));
        }
        
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    // Belirli bir bölümdeki kullanıcıları getir
    public List<User> getUsersByDepartment(Department department) {
        if (department == null) {
            return new ArrayList<>();
        }
        
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> userBelongsToDepartment(user, department))
                .collect(Collectors.toList());
    }
    
    // Kullanıcının belirli bir bölüme ait olup olmadığını kontrol et
    private boolean userBelongsToDepartment(User user, Department department) {
        // Eğitimci olarak bölüme ait mi?
        if (user.getInstructor() != null && 
            user.getInstructor().getDepartment() != null && 
            user.getInstructor().getDepartment().equals(department)) {
            return true;
        }
        
        // Bölüm başkanı olarak bölüme ait mi?
        if (user.getDepartmentHead() != null && 
            user.getDepartmentHead().getDepartment() != null && 
            user.getDepartmentHead().getDepartment().equals(department)) {
            return true;
        }
        
        return false;
    }

    @Transactional
    public void initRoles() {
        if (roleRepository.count() == 0) {
            Role instructorRole = new Role();
            instructorRole.setName("ROLE_INSTRUCTOR");
            instructorRole.setDescription("Eğitimci rolü");
            roleRepository.save(instructorRole);

            Role departmentHeadRole = new Role();
            departmentHeadRole.setName("ROLE_DEPARTMENT_HEAD");
            departmentHeadRole.setDescription("Bölüm Başkanı rolü");
            roleRepository.save(departmentHeadRole);
        }
    }
}