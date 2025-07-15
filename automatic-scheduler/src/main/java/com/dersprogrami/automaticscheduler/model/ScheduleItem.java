package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Entity
@Table(name = "schedule_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"schedule"})
public class ScheduleItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability.DayOfWeek dayOfWeek;
    
    @Column(nullable = false)
    private Integer startHour;
    
    @Column(nullable = false)
    private Integer endHour;
    
    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    // Duration bilgisini döndüren metod
    public Integer getDuration() {
        if (startHour != null && endHour != null) {
            return endHour - startHour;
        }
        return null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleItem that = (ScheduleItem) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}