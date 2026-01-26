package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.service.AssignmentService;
import siwes.project.school_website.service.SubmissionService;
import siwes.project.school_website.service.UserService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Objects;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/lecturer")
@RequiredArgsConstructor
public class LecturerController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final UserService userService;
    private final CourseRepository courseRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Lecturer";
        User lecturer = userService.findByUsername(username).orElseThrow();

        model.addAttribute("assignments", assignmentService.getAllAssignments());
        model.addAttribute("username", username);
        
        // Filter courses taught by this lecturer
        List<Course> myCourses = courseRepository.findAll().stream()
                .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                .collect(Collectors.toList());
        model.addAttribute("courses", myCourses);
        
        return "lecturer/dashboard";
    }

    @GetMapping("/assignment/{id}/submissions")
    public String viewSubmissions(@PathVariable Long id, Model model, Principal principal) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null"));
        Objects.requireNonNull(safeId, "Assignment ID must not be null");
        Assignment assignment = assignmentService.getAssignmentById(safeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        
        // Ownership Protection: Only the creator can view submissions
        if (!assignment.getCreatedBy().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You did not create this assignment.");
        }

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissionService.getSubmissionsForAssignment(id));
        return "lecturer/assignment-submissions";
    }

    @GetMapping("/submission/{id}")
    public String gradeSubmissionView(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null"));
        Submission submission = submissionService.getSubmissionById(safeId);
        model.addAttribute("submission", submission);
        return "lecturer/grading-view";
    }

    @PostMapping("/submission/{id}/grade")
    public String gradeSubmission(@PathVariable Long id, @RequestParam Integer grade, @RequestParam String feedback) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null"));
        submissionService.gradeSubmission(safeId, grade, feedback);

        // Redirect back to the assignment's submission list
        Submission submission = submissionService.getSubmissionById(safeId);
        return "redirect:/lecturer/assignment/" + submission.getAssignment().getId() + "/submissions?graded";
    }

    @GetMapping("/assignment/create")
    public String createAssignmentForm(Model model, Principal principal) {
        String username = principal.getName();
        User lecturer = userService.findByUsername(username).orElseThrow();

        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                .collect(Collectors.toList());

        model.addAttribute("courses", courses);
        model.addAttribute("assignment", new Assignment());
        return "lecturer/create-assignment";
    }

    @PostMapping("/assignment/create")
    @SuppressWarnings("null")
    public String createAssignment(@ModelAttribute Assignment assignment, @RequestParam Long courseId, Principal principal) {
        String username = principal.getName();
        User lecturer = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        Long safeCourseId = Optional.ofNullable(courseId).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeCourseId).orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));

        // Validation: Ensure the lecturer is assigned to this course
        if (course.getLecturer() == null || !course.getLecturer().getId().equals(lecturer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to create assignments for this course.");
        }

        assignment.setCreatedBy(lecturer);
        assignment.setDepartment(course.getDepartment());
        assignment.setCourse(course);
        // Level is bound automatically from the form via @ModelAttribute
        assignmentService.createAssignment(assignment);
        return "redirect:/lecturer/dashboard?created";
    }

    @GetMapping("/submission/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null"));
        Submission submission = submissionService.getSubmissionById(safeId);
        Resource file = submissionService.loadFileAsResource(submission.getSubmissionContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/student/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Student ID cannot be null"));
        User student = userService.findById(safeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + safeId));
        model.addAttribute("student", student);
        return "lecturer/student-profile";
    }

    @GetMapping("/profile-pic/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getProfilePic(@PathVariable String filename) {
        Resource file = userService.loadProfilePic(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(file);
    }

    @GetMapping("/profile")
    public String editProfile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "lecturer/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String phoneNumber,
                                @RequestParam(required = false) MultipartFile file,
                                Principal principal) throws IOException {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();
        userService.updateLecturerProfile(user, fullName, email, phoneNumber, file);
        return "redirect:/lecturer/profile?success";
    }

    @GetMapping("/assignment/delete/{id}")
    public String deleteAssignment(@PathVariable Long id) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null"));
        assignmentService.deleteAssignment(safeId);
        return "redirect:/lecturer/dashboard?deleted";
    }

    @GetMapping("/course/{id}/students")
    @SuppressWarnings("null")
    public String viewEnrolledStudents(@PathVariable Long id, Model model) {
        Long safeId = Optional.ofNullable(id).orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null"));
        Course course = courseRepository.findById(safeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));

        List<User> students = userService.getStudentsByDepartment(course.getDepartment());
        
        model.addAttribute("course", course);
        model.addAttribute("students", students);
        return "course-students";
    }
}