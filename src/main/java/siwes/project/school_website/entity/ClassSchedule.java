package siwes.project.school_website.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
public class ClassSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
}