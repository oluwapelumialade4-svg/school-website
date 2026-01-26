package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.UserRepository;
import siwes.project.school_website.repository.NotificationRepository;
import siwes.project.school_website.repository.CourseMaterialRepository;
import siwes.project.school_website.repository.ClassScheduleRepository;
import siwes.project.school_website.repository.ForumPostRepository;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Optional;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationRepository notificationRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ForumPostRepository forumPostRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Debug Logging for Render
        System.out.println("DEBUG: Student Dashboard Accessed");
        System.out.println("DEBUG: Student: " + student.getUsername());
        System.out.println("DEBUG: Department: " + (student.getDepartment() != null ? student.getDepartment().getName() : "N/A"));
        System.out.println("DEBUG: Level: " + student.getLevel());

        model.addAttribute("user", student);
        
        String level = (student.getLevel() != null) ? student.getLevel() : "";

        // Fetch assignments matching the student's Department entity and level (case-insensitive ensured by using the entity)
        model.addAttribute("assignments", assignmentRepository.findByDepartmentAndLevel(student.getDepartment(), level));
        model.addAttribute("courses", student.getRegisteredCourses());
        model.addAttribute("notifications", notificationRepository.findByRecipientOrderByTimestampDesc(student));

        return "student/dashboard";
    }

    @GetMapping("/course/register")
    public String showCourseRegistration(Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();
        
        if (student.getDepartment() == null) {
            model.addAttribute("courses", Collections.emptyList());
            model.addAttribute("student", student);
            return "student/register-course";
        }

        // Fetch all courses in the student's department
        // Note: Assuming findByDepartment exists or filtering manually
        List<Course> departmentCourses = courseRepository.findAll().stream()
                .filter(c -> c.getDepartment() != null && c.getDepartment().getId().equals(student.getDepartment().getId()))
                .collect(Collectors.toList());

        model.addAttribute("courses", departmentCourses);
        model.addAttribute("student", student);
        return "student/register-course";
    }

    @PostMapping("/course/register")
    public String registerCourse(@RequestParam Long courseId, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();
        Long safeCourseId = Optional.ofNullable(courseId).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeCourseId).orElseThrow();

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
        Long safeCourseId = Optional.ofNullable(courseId).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeCourseId).orElseThrow();

        if (student.getRegisteredCourses().contains(course)) {
            student.getRegisteredCourses().remove(course);
            userRepository.save(student);
        }
        return "redirect:/student/dashboard?dropped";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute User formData,
                                @RequestParam(required = false) MultipartFile file,
                                Principal principal) throws IOException {
        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElseThrow();

        user.setFullName(formData.getFullName());
        user.setEmail(formData.getEmail());
        user.setPhoneNumber(formData.getPhoneNumber());

        if (file != null && !file.isEmpty()) {
            String filename = System.currentTimeMillis() + "_profile_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), Paths.get("uploads").resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePic(filename);
        }

        userRepository.save(user);
        return "redirect:/student/profile?success";
    }

    @GetMapping("/course/{id}/materials")
    public String viewCourseMaterials(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeId).orElseThrow();
        model.addAttribute("course", course);
        model.addAttribute("materials", courseMaterialRepository.findByCourse(course));
        return "student/course-materials";
    }

    @GetMapping("/course/{id}/schedule")
    public String viewClassSchedule(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeId).orElseThrow();
        model.addAttribute("course", course);
        model.addAttribute("schedules", classScheduleRepository.findByCourse(course));
        return "student/class-schedule";
    }

    @GetMapping("/course/{id}/forum")
    public String viewCourseForum(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeId).orElseThrow();
        model.addAttribute("course", course);
        model.addAttribute("posts", forumPostRepository.findByCourseOrderByTimestampDesc(course));
        return "student/course-forum";
    }
}