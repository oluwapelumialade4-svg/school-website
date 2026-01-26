package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.service.AssignmentService;
import siwes.project.school_website.service.SubmissionService;
import siwes.project.school_website.service.UserService;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.ClassSchedule;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.repository.ClassScheduleRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final UserService userService;
    private final CourseRepository courseRepository;
    private final ClassScheduleRepository classScheduleRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : "Student";
        User user = userService.findByUsername(username).orElse(new User());
        if (user.getDepartment() != null) {
            model.addAttribute("assignments", assignmentService.getAssignmentsByDepartmentAndLevel(user.getDepartment(), user.getLevel()));
            
            List<Submission> submissions = submissionService.getSubmissionsForStudent(user);
            Set<Long> submittedAssignmentIds = submissions.stream()
                    .map(s -> s.getAssignment().getId())
                    .collect(Collectors.toSet());
            model.addAttribute("submittedAssignmentIds", submittedAssignmentIds);
            model.addAttribute("submissions", submissions);

            // Calculate Average Grade
            double average = submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .mapToInt(Submission::getGrade)
                    .average()
                    .orElse(0.0);
            model.addAttribute("averageGrade", (int) Math.round(average));
        } else {
            model.addAttribute("assignments", Collections.emptyList());
            model.addAttribute("submittedAssignmentIds", Collections.emptySet());
            model.addAttribute("submissions", Collections.emptyList());
            model.addAttribute("averageGrade", 0);
        }
        model.addAttribute("username", username);
        model.addAttribute("user", user);
        return "student/dashboard";
    }

    @GetMapping("/assignment/{id}")
    public String viewAssignment(@PathVariable Long id, Model model, java.security.Principal principal) {
        String username = principal != null ? principal.getName() : "Student";

        Assignment assignment = assignmentService.getAssignmentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        Optional<Submission> submission = submissionService.getSubmission(id, username);

        model.addAttribute("assignment", assignment);
        model.addAttribute("submission", submission.orElse(null));

        return "student/assignment-view";
    }

    @PostMapping("/assignment/{id}/submit")
    public String submitAssignment(@PathVariable Long id, @RequestParam("file") MultipartFile file, java.security.Principal principal) {
        if (file.isEmpty()) {
            return "redirect:/student/assignment/" + id + "?error";
        }
        String username = principal != null ? principal.getName() : "Student";
        try {
            submissionService.submitAssignment(id, username, file);
        } catch (IOException e) {
            return "redirect:/student/assignment/" + id + "?error";
        } catch (IllegalArgumentException e) {
            return "redirect:/student/assignment/" + id + "?error=type";
        }
        return "redirect:/student/assignment/" + id + "?success";
    }

    @GetMapping("/submission/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id, java.security.Principal principal) {
        Submission submission = submissionService.getSubmissionById(id);
        String username = principal != null ? principal.getName() : "";
        if (!submission.getStudent().getUsername().equals(username)) {
            throw new IllegalArgumentException("Access Denied");
        }
        Resource file = submissionService.loadFileAsResource(submission.getSubmissionContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/course/{courseId}/schedule")
    public String viewClassSchedule(@PathVariable Long courseId, Model model, java.security.Principal principal) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Get the current user
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();

        // Check if the student is enrolled in this course (you might want to add enrollment logic)
        // For now, we'll allow access if the user is a student

        List<ClassSchedule> schedules = classScheduleRepository.findByCourse(course);

        model.addAttribute("course", course);
        model.addAttribute("schedules", schedules);

        return "class-schedule";
    }

    @GetMapping("/profile")
    public String editProfile(Model model, java.security.Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phoneNumber,
                                @RequestParam String matricNumber,
                                @RequestParam Integer age,
                                @RequestParam String level,
                                @RequestParam String department,
                                @RequestParam(required = false) MultipartFile file,
                                java.security.Principal principal) throws IOException {
        // Validate Matric Number (Must be exactly 7 digits, e.g., 2403082)
        if (!matricNumber.matches("\\d{7}")) {
            return "redirect:/student/profile?error=invalidMatric";
        }

        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();

        // Check for duplicate matric number
        Optional<User> existingUser = userService.findByMatricNumber(matricNumber);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
            return "redirect:/student/profile?error=duplicateMatric";
        }

        userService.updateStudentProfile(user, fullName, email, phoneNumber, matricNumber, age, level, department, file);
        return "redirect:/student/profile?success";
    }

    @GetMapping("/profile-pic/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getProfilePic(@PathVariable String filename) {
        Resource file = userService.loadProfilePic(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(file);
    }
}