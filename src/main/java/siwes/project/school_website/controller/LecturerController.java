package siwes.project.school_website.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.entity.Course;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.CourseRepository;
import siwes.project.school_website.service.AssignmentService;
import siwes.project.school_website.service.SubmissionService;
import siwes.project.school_website.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Objects;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;

@Controller
@RequiredArgsConstructor
public class LecturerController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final UserService userService;
    private final CourseRepository courseRepository;

    @GetMapping("/lecturer/dashboard")
    @SuppressWarnings("null")
    public String dashboard(Model model, Principal principal) {
        String username = principal != null ? principal.getName() : "Lecturer";
        User lecturer = userService.findByUsername(username).orElseThrow();

        List<Assignment> assignments = assignmentService.getAllAssignments();

        model.addAttribute("assignments", assignments);
        model.addAttribute("username", username);
        
        // Filter courses taught by this lecturer
        List<Course> myCourses = courseRepository.findAll().stream()
                .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                .collect(Collectors.toList());
        model.addAttribute("courses", myCourses);
        
        return "lecturer/dashboard";
    }

    @GetMapping("/lecturer/assignment/{id}/submissions")
    @SuppressWarnings("null")
    public String viewSubmissions(@PathVariable Long id, Model model, Principal principal) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment ID cannot be null");
        }
        Assignment assignment = assignmentService.getAssignmentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        
        // Ownership Protection: Only the creator can view submissions
        if (!assignment.getCreatedBy().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You did not create this assignment.");
        }

        model.addAttribute("assignment", assignment);
        model.addAttribute("submissions", submissionService.getSubmissionsForAssignment(id));
        return "lecturer/assignment-submissions";
    }

    @GetMapping("/lecturer/submission/{id}")
    public String gradeSubmissionView(@PathVariable Long id, Model model) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        Submission submission = submissionService.getSubmissionById(id);
        model.addAttribute("submission", submission);
        return "lecturer/grading-view";
    }

    @PostMapping("/lecturer/submission/{id}/grade")
    public String gradeSubmission(@PathVariable Long id, @RequestParam Integer grade, @RequestParam String feedback) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        submissionService.gradeSubmission(id, grade, feedback);

        // Redirect back to the assignment's submission list
        Submission submission = submissionService.getSubmissionById(id);
        return "redirect:/lecturer/assignment/" + submission.getAssignment().getId() + "/submissions?graded";
    }

    @GetMapping("/lecturer/assignment/create")
    @SuppressWarnings("null")
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

    @PostMapping("/lecturer/assignment/create")
    @SuppressWarnings("null")
    public String createAssignment(@Valid @ModelAttribute Assignment assignment, 
                                   BindingResult bindingResult,
                                   @RequestParam(required = false) Long courseId, 
                                   @RequestParam(defaultValue = "publish") String action,
                                   Principal principal,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            // Reload courses for the form
            String username = principal.getName();
            User lecturer = userService.findByUsername(username).orElseThrow();
            List<Course> courses = courseRepository.findAll().stream()
                    .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("courses", courses);
            return "lecturer/create-assignment";
        }

        try {
            if (courseId == null) {
                return "redirect:/lecturer/assignment/create?error=Please select a valid course";
            }

            String username = principal.getName();
            User lecturer = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Lecturer not found"));

            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return "redirect:/lecturer/assignment/create?error=Invalid Course Selected";
            }

            // Validation: Ensure the lecturer is assigned to this course
            if (course.getLecturer() == null || !Objects.equals(course.getLecturer().getId(), lecturer.getId())) {
                return "redirect:/lecturer/dashboard?error=You are not authorized to create assignments for this course.";
            }

            // Additional validation
            if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
                return "redirect:/lecturer/assignment/create?error=Assignment title cannot be empty";
            }
            if (assignment.getDueDate() == null) {
                return "redirect:/lecturer/assignment/create?error=Due date cannot be null";
            }

            assignment.setCreatedBy(lecturer);
            assignment.setDepartment(course.getDepartment());
            assignment.setCourse(course);
            
            // Level is bound automatically from the form via @ModelAttribute
            assignmentService.createAssignment(assignment);
            return "redirect:/lecturer/dashboard?created";
        } catch (Exception e) {
            // Log the error and redirect with error message
            System.err.println("Error creating assignment: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/lecturer/dashboard?error=" + e.getMessage();
        }
    }

    @PostMapping("/lecturer/assignment/update")
    @SuppressWarnings("null")
    public String updateAssignment(@RequestParam Long id,
                                   @ModelAttribute Assignment formData,
                                   @RequestParam(required = false) String action,
                                   Principal principal) {
        String username = principal.getName();
        Assignment assignment = assignmentService.getAssignmentById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (!assignment.getCreatedBy().getUsername().equals(username)) {
            return "redirect:/lecturer/dashboard?error=Unauthorized";
        }

        assignment.setTitle(formData.getTitle());
        assignment.setDescription(formData.getDescription());
        assignment.setDueDate(formData.getDueDate());

        assignmentService.createAssignment(assignment); // Saves the updated entity
        return "redirect:/lecturer/dashboard?updated";
    }

    @GetMapping("/lecturer/submission/{id}/download")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID cannot be null");
        }
        Submission submission = submissionService.getSubmissionById(id);
        Resource file = submissionService.loadFileAsResource(submission.getSubmissionContent());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @GetMapping("/lecturer/student/{id}")
    public String viewStudentProfile(@PathVariable Long id, Model model) {
        if (id == null) {
            throw new IllegalArgumentException("Student ID cannot be null");
        }
        User student = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + id));
        model.addAttribute("student", student);
        return "lecturer/student-profile";
    }

    @GetMapping("/lecturer/profile-pic/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getProfilePic(@PathVariable String filename) {
        Resource file = userService.loadProfilePic(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/jpeg")
                .body(file);
    }

    @GetMapping("/lecturer/profile")
    @SuppressWarnings("null")
    public String editProfile(Model model, Principal principal) {
        String username = principal.getName();
        User user = userService.findByUsername(username).orElseThrow();
        model.addAttribute("user", user);
        return "lecturer/profile";
    }

    @PostMapping("/lecturer/profile")
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

    @GetMapping("/lecturer/assignment/delete/{id}")
    public String deleteAssignment(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Assignment ID cannot be null");
        }
        assignmentService.deleteAssignment(id);
        return "redirect:/lecturer/dashboard?deleted";
    }

    @PostMapping("/lecturer/assignment/delete-bulk")
    public String deleteBulkAssignments(@RequestParam(required = false) List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            for (Long id : ids) {
                assignmentService.deleteAssignment(id);
            }
        }
        return "redirect:/lecturer/dashboard?deleted";
    }

    @GetMapping("/lecturer/course/{id}/students")
    public String viewEnrolledStudents(@PathVariable Long id, Model model) {
        if (id == null) {
            throw new IllegalArgumentException("Course ID cannot be null");
        }
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Course ID"));

        List<User> students = userService.getStudentsByDepartment(course.getDepartment());

        model.addAttribute("course", course);
        model.addAttribute("students", students);
        return "course-students";
    }
}