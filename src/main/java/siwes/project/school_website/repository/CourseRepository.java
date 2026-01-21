package siwes.project.school_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
}