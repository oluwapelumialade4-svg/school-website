package siwes.project.school_website.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.UserRepository;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Only initialize if the database is empty
            if (userRepository.count() == 0) {
                User student = new User();
                student.setUsername("student");
                student.setPassword(passwordEncoder.encode("password"));
                student.setRole(Role.STUDENT);
                student.setFullName("Test Student");
                student.setEmail("student@school.com");
                userRepository.save(student);

                User lecturer = new User();
                lecturer.setUsername("lecturer");
                lecturer.setPassword(passwordEncoder.encode("password"));
                lecturer.setRole(Role.LECTURER);
                lecturer.setFullName("Test Lecturer");
                lecturer.setEmail("lecturer@school.com");
                userRepository.save(lecturer);
            }
        };
    }
}