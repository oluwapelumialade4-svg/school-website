package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.UserRepository;
import siwes.project.school_website.repository.NotificationRepository;
import siwes.project.school_website.repository.CourseMaterialRepository;
import siwes.project.school_website.repository.ClassScheduleRepository;
import siwes.project.school_website.repository.ForumPostRepository;
import siwes.project.school_website.service.SubmissionService;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.Optional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

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
    private final SubmissionService submissionService;

    @GetMapping("/dashboard")
    @SuppressWarnings("null")
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
        model.addAttribute("username", student.getFullName());
        
        String level = (student.getLevel() != null) ? student.getLevel() : "";

        // Fetch assignments matching the student's Department name (case-insensitive) and level
        List<Assignment> assignments = (student.getDepartment() != null) ? 
            assignmentRepository.findByDepartment_NameIgnoreCaseAndLevel(student.getDepartment().getName(), level) : 
            Collections.emptyList();
        model.addAttribute("assignments", assignments);
        
        // Get submitted assignment IDs
        List<Long> submittedAssignmentIds = submissionService.getSubmissionsForStudent(student).stream()
                .map(sub -> sub.getAssignment().getId())
                .collect(Collectors.toList());
        model.addAttribute("submittedAssignmentIds", submittedAssignmentIds);
        
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

    @GetMapping("/assignment/{id}")
    public String viewAssignment(@PathVariable Long id, Model model, Principal principal) {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();

        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null"));
        Assignment assignment = assignmentRepository.findById(safeId).orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        // Check if assignment matches student's department and level
        if (student.getDepartment() == null || !student.getDepartment().equals(assignment.getDepartment()) || !assignment.getLevel().equals(student.getLevel())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        model.addAttribute("assignment", assignment);

        // Check if already submitted
        Optional<Submission> existingSubmission = submissionService.getSubmissionByStudentAndAssignment(student, assignment);
        model.addAttribute("submitted", existingSubmission.isPresent());
        if (existingSubmission.isPresent()) {
            model.addAttribute("submission", existingSubmission.get());
        }

        return "student/assignment-view";
    }

    @PostMapping("/assignment/{id}/submit")
    public String submitAssignment(@PathVariable Long id, @RequestParam MultipartFile file, Principal principal) throws IOException {
        String username = principal.getName();
        User student = userRepository.findByUsername(username).orElseThrow();

        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null"));
        Assignment assignment = assignmentRepository.findById(safeId).orElseThrow(() -> new IllegalArgumentException("Assignment not found"));

        // Check access
        if (student.getDepartment() == null || !student.getDepartment().equals(assignment.getDepartment()) || !assignment.getLevel().equals(student.getLevel())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (file.isEmpty()) {
            return "redirect:/student/assignment/" + id + "?error=FileRequired";
        }

        submissionService.submitAssignment(assignment.getId(), username, file);

        return "redirect:/student/assignment/" + id + "?success";
    }

    @GetMapping("/material/{id}/download")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id) throws IOException {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Material ID cannot be null"));
        // Assuming CourseMaterialRepository is injected, but it's not. Wait, in the code, it's courseMaterialRepository
        // But in StudentController, it is injected.
        var material = courseMaterialRepository.findById(safeId).orElseThrow(() -> new IllegalArgumentException("Material not found"));
        Path filePath = Paths.get("uploads").resolve(material.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + material.getOriginalFileName() + "\"")
                    .body(resource);
        } else {
            throw new RuntimeException("Could not read file: " + material.getOriginalFileName());
        }
    }

    @GetMapping("/submission/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id, Principal principal) {
        String username = principal.getName();

        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null"));
        Submission submission = submissionService.getSubmissionById(safeId);

        // Ensure the submission belongs to the student
        if (!submission.getStudent().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Resource file = submissionService.loadFileAsResource(submission.getSubmissionContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + submission.getSubmissionContent() + "\"")
                .body(file);
    }

    @GetMapping("/test")
    public String test(Model model) {
        List<User> users = userRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        model.addAttribute("users", users);
        model.addAttribute("assignments", assignments);
        return "test";
    }
}