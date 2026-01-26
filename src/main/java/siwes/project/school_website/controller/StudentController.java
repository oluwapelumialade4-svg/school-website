package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.UserRepository;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        model.addAttribute("student", student);
        // Fetch assignments matching the student's department and level
        model.addAttribute("assignments", assignmentRepository.findByDepartmentAndLevel(student.getDepartment(), student.getLevel()));
        model.addAttribute("courses", student.getRegisteredCourses());

        return "student/dashboard";
    }

    @GetMapping("/course/register")
    public String showCourseRegistration(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();
        
        // Fetch all courses in the student's department
        // Note: Assuming findByDepartment exists or filtering manually
        List<Course> departmentCourses = courseRepository.findAll().stream()
                .filter(c -> c.getDepartment().getId().equals(student.getDepartment().getId()))
                .collect(Collectors.toList());

        model.addAttribute("courses", departmentCourses);
        model.addAttribute("student", student);
        return "student/register-course";
    }

    @PostMapping("/course/register")
    public String registerCourse(@RequestParam Long courseId, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();

        // Calculate current total credit units
        int currentUnits = student.getRegisteredCourses().stream()
                .mapToInt(c -> c.getCreditUnits() != null ? c.getCreditUnits() : 0)
                .sum();
        int newUnits = course.getCreditUnits() != null ? course.getCreditUnits() : 0;
        int maxUnits = 24; // Set maximum credit unit limit

        if (!student.getRegisteredCourses().contains(course)) {
            if (currentUnits + newUnits > maxUnits) {
                return "redirect:/student/dashboard?error=CreditLimitExceeded";
            }
            student.getRegisteredCourses().add(course);
            userRepository.save(student);
        }
        return "redirect:/student/dashboard?registered";
    }

    @PostMapping("/course/drop")
    public String dropCourse(@RequestParam Long courseId, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();

        if (student.getRegisteredCourses().contains(course)) {
            student.getRegisteredCourses().remove(course);
            userRepository.save(student);
        }
        return "redirect:/student/dashboard?dropped";
    }
}