package siwes.project.school_website.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import siwes.project.school_website.entity.Role;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Department;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.repository.UserRepository;
import siwes.project.school_website.repository.DepartmentRepository;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.AssignmentRepository;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_PASSWORD:admin123}")
    private String adminPassword;

    @EventListener(ContextRefreshedEvent.class)
    public void initData() {
        // Ensure Admin exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setFullName("System Admin");
            admin.setEmail("admin@school.com");
            userRepository.save(admin);
        }

        // Ensure Department exists
        Department department = departmentRepository.findByName("Computer Science");
        if (department == null) {
            department = new Department();
            department.setName("Computer Science");
            departmentRepository.save(department);
        }

        // Initialize other users if they don't exist
        if (userRepository.findByUsername("student").isEmpty()) {
            User student = new User();
            student.setUsername("student");
            student.setPassword(passwordEncoder.encode("password"));
            student.setRole(Role.STUDENT);
            student.setFullName("Test Student");
            student.setEmail("student@school.com");
            student.setLevel("100L");
            student.setDepartment(department);
            userRepository.save(student);
        }

        User lecturer = null;
        if (userRepository.findByUsername("lecturer").isEmpty()) {
            lecturer = new User();
            lecturer.setUsername("lecturer");
            lecturer.setPassword(passwordEncoder.encode("password"));
            lecturer.setRole(Role.LECTURER);
            lecturer.setFullName("Test Lecturer");
            lecturer.setEmail("lecturer@school.com");
            lecturer.setLevel("100L"); // Lecturer handles 100L
            userRepository.save(lecturer);
        } else {
            lecturer = userRepository.findByUsername("lecturer").get();
        }

        // Ensure Course exists and is assigned
        if (courseRepository.count() == 0) {
            Course course = new Course();
            course.setName("Intro to Java");
            course.setCourseCode("CSC101");
            course.setCreditUnits(3);
            course.setDepartment(department);
            course.setLecturer(lecturer);
            courseRepository.save(course);

            // Create an assignment for this course/level
            Assignment assignment = new Assignment();
            assignment.setTitle("Java Basics");
            assignment.setDescription("Complete Chapter 1 exercises.");
            assignment.setDueDate(LocalDate.now().plusDays(7));
            assignment.setDepartment(department);
            assignment.setCourse(course);
            assignment.setCreatedBy(lecturer);
            assignment.setLevel("100L");
            assignmentRepository.save(assignment);
        }
    }
}