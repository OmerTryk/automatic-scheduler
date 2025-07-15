package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"instructors", "scheduleItems"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer weeklyHours;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @Column(nullable = false)
    private Integer credits = 3; // varsayılan değer 3
    
    @Column
    private Boolean isElective = false;
    
    @Column
    private String description;
    
    @ManyToMany
    @JoinTable(name = "course_instructors", joinColumns = @JoinColumn(name = "course_id"), inverseJoinColumns = @JoinColumn(name = "instructor_id"))
    private Set<Instructor> instructors = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ScheduleItem> scheduleItems = new HashSet<>();

    public Instructor getInstructor() {
        if (instructors == null || instructors.isEmpty()) {
            return null;
        }
        return instructors.iterator().next();
    }

    public void setInstructor(Instructor instructor) {
        this.instructors.clear();
        if (instructor != null) {
            this.instructors.add(instructor);
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}