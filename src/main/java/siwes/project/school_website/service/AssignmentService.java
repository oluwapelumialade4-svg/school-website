package siwes.project.school_website.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.repository.AssignmentRepository;
import siwes.project.school_website.entity.Department;

import java.util.List;
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
        return assignmentRepository.findByCourse_Department(department);
    }

    public List<Assignment> getAssignmentsByDepartmentAndLevel(Department department, String level) {
        return assignmentRepository.findByDepartmentAndLevel(department, level);
    }

    @SuppressWarnings("null")
    public void createAssignment(Assignment assignment) {
        assignmentRepository.save(assignment);
    }

    public Optional<Assignment> getAssignmentById(Long id) {
        @SuppressWarnings("null")
        Optional<Assignment> result = assignmentRepository.findById(id);
        return result;
    }

    @SuppressWarnings("null")
    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(id);
    }
}
