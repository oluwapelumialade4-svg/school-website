package siwes.project.school_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.Assignment;
import siwes.project.school_website.entity.Department;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    // Fetch assignments where the course belongs to the student's department
    List<Assignment> findByCourse_Department(Department department);

    // Fetch assignments for a specific course
    List<Assignment> findByCourseId(Long courseId);
}