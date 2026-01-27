package siwes.project.school_website.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Submission;
import siwes.project.school_website.entity.User;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.repository.SubmissionRepository;
import siwes.project.school_website.repository.UserRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final Path rootLocation = Paths.get("uploads");

    public void submitAssignment(Long assignmentId, String username, MultipartFile file) throws IOException {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        Submission submission = submissionRepository.findByStudentAndAssignment(student, assignment)
                .orElse(new Submission());

        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        String contentType = file.getContentType();
        if (contentType != null && 
                !contentType.contains("pdf") && 
                !contentType.contains("word") && 
                !contentType.contains("document") && 
                !contentType.contains("msword")) {
            throw new IllegalArgumentException("Invalid file type. Only PDF and Word documents are allowed.");
        }

        Files.createDirectories(rootLocation);
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setSubmissionContent(filename);

        submissionRepository.save(submission);
    }

    @SuppressWarnings("null")
    public List<Submission> getSubmissionsForAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));
        return submissionRepository.findByAssignment(assignment);
    }

    @SuppressWarnings("null")
    public List<Submission> getSubmissionsForStudent(User student) {
        return submissionRepository.findByStudent(student);
    }

    public void gradeSubmission(Long submissionId, Integer grade, String feedback) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submissionRepository.save(submission);
    }

    @SuppressWarnings("null")
    public Submission getSubmissionById(Long id) {
        Submission result = submissionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));
        return result;
    }

    public Resource loadFileAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            @SuppressWarnings("null")
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    public Optional<Submission> getSubmission(Long id, String username) {
        Assignment assignment = assignmentRepository.findById(id).orElse(null);
        User student = userRepository.findByUsername(username).orElse(null);
        if (assignment != null && student != null) {
            return submissionRepository.findByStudentAndAssignment(student, assignment);
        }
        return Optional.empty();
    }

    public Optional<Submission> getSubmissionByStudentAndAssignment(User student, Assignment assignment) {
        return submissionRepository.findByStudentAndAssignment(student, assignment);
    }
}
