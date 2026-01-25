package siwes.project.school_website.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ContextRefreshedEvent.class)
    public void initData() {
        // Ensure Admin exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            admin.setFullName("System Admin");
            admin.setEmail("admin@school.com");
            userRepository.save(admin);
        }

        // Initialize other users if they don't exist
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("password"));
            student.setRole(Role.STUDENT);
            student.setFullName("Test Student");
            student.setEmail("student@school.com");
            userRepository.save(student);
        }

        if (userRepository.findByUsername("lecturer").isEmpty()) {
            User lecturer = new User();
            lecturer.setUsername("lecturer");
            lecturer.setPassword(passwordEncoder.encode("password"));
            lecturer.setRole(Role.LECTURER);
            lecturer.setFullName("Test Lecturer");
            lecturer.setEmail("lecturer@school.com");
            userRepository.save(lecturer);
        }
    }
}