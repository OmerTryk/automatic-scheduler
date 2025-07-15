package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"scheduleItems"})
public class Schedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
    
    @Column(nullable = false)
    private String academicTerm;
    
    @Column
    private String description;
    
    @Column
    private Boolean isActive = false;
    
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ScheduleItem> scheduleItems = new HashSet<>();
    
    // Yardımcı metod: Program öğesi eklemek için
    public void addScheduleItem(ScheduleItem item) {
        if (scheduleItems == null) {
            scheduleItems = new HashSet<>();
        }
        scheduleItems.add(item);
        item.setSchedule(this);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return Objects.equals(id, schedule.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}