package siwes.project.school_website.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import siwes.project.school_website.entity.ClassSchedule;
import siwes.project.school_website.entity.Course;
import java.util.List;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByCourse(Course course);
}