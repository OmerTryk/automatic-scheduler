package com.dersprogrami.automaticscheduler.config;

import com.dersprogrami.automaticscheduler.model.*;
import com.dersprogrami.automaticscheduler.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentHeadRepository departmentHeadRepository;
    private final InstructorRepository instructorRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                          UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          DepartmentHeadRepository departmentHeadRepository,
                          InstructorRepository instructorRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.departmentHeadRepository = departmentHeadRepository;
        this.instructorRepository = instructorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Eğer roller zaten oluşturulmuşsa tekrar oluşturma
        if (roleRepository.count() == 0) {
            // Rolleri oluştur
            Role instructorRole = new Role();
            instructorRole.setName("ROLE_INSTRUCTOR");
            instructorRole.setDescription("Eğitimci rolü");
            roleRepository.save(instructorRole);

            Role departmentHeadRole = new Role();
            departmentHeadRole.setName("ROLE_DEPARTMENT_HEAD");
            departmentHeadRole.setDescription("Bölüm Başkanı rolü");
            roleRepository.save(departmentHeadRole);

            // Örnek bölümleri oluştur
            Department computerEngineering = new Department();
            computerEngineering.setName("Bilgisayar Mühendisliği");
            computerEngineering.setDescription("Bilgisayar Mühendisliği Bölümü");
            departmentRepository.save(computerEngineering);

            Department electricalEngineering = new Department();
            electricalEngineering.setName("Elektrik-Elektronik Mühendisliği");
            electricalEngineering.setDescription("Elektrik-Elektronik Mühendisliği Bölümü");
            departmentRepository.save(electricalEngineering);

            // Örnek bölüm başkanı kullanıcısı oluştur
            User departmentHeadUser = new User();
            departmentHeadUser.setUsername("bolum_baskani");
            departmentHeadUser.setPassword(passwordEncoder.encode("password"));
            departmentHeadUser.setEmail("bolum_baskani@example.com");
            departmentHeadUser.setFullName("Prof. Dr. Ahmet Yılmaz");
            
            Set<Role> departmentHeadRoles = new HashSet<>();
            departmentHeadRoles.add(departmentHeadRole);
            departmentHeadUser.setRoles(departmentHeadRoles);
            
            userRepository.save(departmentHeadUser);

            // Bilgisayar Mühendisliği bölüm başkanını ata
            DepartmentHead departmentHead = new DepartmentHead();
            departmentHead.setUser(departmentHeadUser);
            departmentHead.setDepartment(computerEngineering);
            departmentHeadRepository.save(departmentHead);

            // Örnek eğitimci kullanıcıları oluştur
            User instructorUser1 = new User();
            instructorUser1.setUsername("egitimci1");
            instructorUser1.setPassword(passwordEncoder.encode("password"));
            instructorUser1.setEmail("egitimci1@example.com");
            instructorUser1.setFullName("Dr. Mehmet Öztürk");
            
            Set<Role> instructorRoles = new HashSet<>();
            instructorRoles.add(instructorRole);
            instructorUser1.setRoles(instructorRoles);
            
            userRepository.save(instructorUser1);

            User instructorUser2 = new User();
            instructorUser2.setUsername("egitimci2");
            instructorUser2.setPassword(passwordEncoder.encode("password"));
            instructorUser2.setEmail("egitimci2@example.com");
            instructorUser2.setFullName("Dr. Ayşe Demir");
            
            instructorUser2.setRoles(new HashSet<>(instructorRoles));
            
            userRepository.save(instructorUser2);

            // Eğitimcileri bölümlere ata
            Instructor instructor1 = new Instructor();
            instructor1.setUser(instructorUser1);
            instructor1.setDepartment(computerEngineering);
            instructorRepository.save(instructor1);

            Instructor instructor2 = new Instructor();
            instructor2.setUser(instructorUser2);
            instructor2.setDepartment(computerEngineering);
            instructorRepository.save(instructor2);
        }
    }
}