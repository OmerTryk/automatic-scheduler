package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "availabilities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Availability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;
    
    @Column(nullable = false)
    private Integer startHour;
    
    @Column(nullable = false)
    private Integer endHour;
    
    public enum DayOfWeek {
        MONDAY("Pazartesi"),
        TUESDAY("Salı"),
        WEDNESDAY("Çarşamba"),
        THURSDAY("Perşembe"),
        FRIDAY("Cuma");
        
        private final String turkishName;
        
        DayOfWeek(String turkishName) {
            this.turkishName = turkishName;
        }
        
        public String getTurkishName() {
            return turkishName;
        }
    }
}