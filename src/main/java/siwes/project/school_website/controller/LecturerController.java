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
import siwes.project.school_website.entity.AssignmentStatus;
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
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        User lecturer = userService.findByUsername(username).orElseThrow();

        // Get only assignments created by this lecturer
        List<Assignment> assignments = assignmentService.getAllAssignments().stream()
                .filter(a -> a.getCreatedBy() != null && a.getCreatedBy().getId().equals(lecturer.getId()))
                .collect(Collectors.toList());

        model.addAttribute("assignments", assignments);
        model.addAttribute("username", lecturer.getFullName());
        
        List<Course> myCourses = courseRepository.findAll().stream()
                .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                .collect(Collectors.toList());
        model.addAttribute("courses", myCourses);
        
        return "lecturer/dashboard";
    }

    @GetMapping("/lecturer/assignment/{id}/submissions")
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
    public String createAssignment(@Valid @ModelAttribute Assignment assignment, 
                                   BindingResult bindingResult,
                                   @RequestParam(required = false) Long courseId, 
                                   @RequestParam(defaultValue = "publish") String action,
                                   Principal principal,
                                   Model model) {
        String username = principal.getName();
        
        if (bindingResult.hasErrors()) {
            User lecturer = userService.findByUsername(username).orElseThrow();
            List<Course> courses = courseRepository.findAll().stream()
                    .filter(c -> c.getLecturer() != null && c.getLecturer().getId().equals(lecturer.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("courses", courses);
            return "lecturer/create-assignment";
        }

        try {
            if (courseId == null) {
                return "redirect:/lecturer/assignment/create?error=Please%20select%20a%20valid%20course";
            }

            if (assignment.getLevel() == null || assignment.getLevel().trim().isEmpty()) {
                return "redirect:/lecturer/assignment/create?error=Please%20select%20a%20level";
            }

            User lecturer = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Lecturer not found"));

            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Course Selected"));

            if (!Objects.equals(course.getLecturer().getId(), lecturer.getId())) {
                return "redirect:/lecturer/dashboard?error=Unauthorized";
            }

            if (assignment.getTitle() == null || assignment.getTitle().trim().isEmpty()) {
                return "redirect:/lecturer/assignment/create?error=Assignment%20title%20cannot%20be%20empty";
            }
            
            if (assignment.getDueDate() == null) {
                return "redirect:/lecturer/assignment/create?error=Due%20date%20cannot%20be%20null";
            }

            assignment.setCreatedBy(lecturer);
            assignment.setDepartment(course.getDepartment());
            assignment.setCourse(course);
            
            if ("draft".equalsIgnoreCase(action)) {
                assignment.setStatus(AssignmentStatus.DRAFT);
            } else {
                assignment.setStatus(AssignmentStatus.PUBLISHED);
            }

            assignmentService.createAssignment(assignment);
            return "redirect:/lecturer/dashboard?created";
        } catch (Exception e) {
            System.err.println("Error creating assignment: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                // Sanitize error message to prevent URL header issues (newlines) which cause 500 errors
                errorMsg = errorMsg.split("\n")[0];
                if (errorMsg.length() > 100) errorMsg = errorMsg.substring(0, 100) + "...";
            }
            return "redirect:/lecturer/dashboard?error=" + errorMsg;
        }
    }

    @PostMapping("/lecturer/assignment/update")
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

        if ("publish".equalsIgnoreCase(action)) {
            assignment.setStatus(AssignmentStatus.PUBLISHED);
        }

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