package siwes.project.school_website.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.entity.Department;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public List<Assignment> getAssignmentsByDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        return assignmentRepository.findByCourse_Department(department);
    }

    public List<Assignment> getAssignmentsByDepartmentAndLevel(Department department, String level) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException("Level cannot be null or empty");
        }
        return assignmentRepository.findByDepartmentAndLevel(department, level);
    }

    public void createAssignment(Assignment assignment) {
        assignmentRepository.save(Objects.requireNonNull(assignment, "Assignment cannot be null"));
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        return assignmentRepository.findById(Objects.requireNonNull(id, "ID cannot be null"));
    }

    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(Objects.requireNonNull(id, "ID cannot be null"));
    }
}
