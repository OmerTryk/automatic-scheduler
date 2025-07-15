package com.dersprogrami.automaticscheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"departmentHead", "courses", "instructors"})
@EqualsAndHashCode(of = {"id", "name"}, callSuper = false)
public class Department {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column
    private String description;
    
    @OneToOne(mappedBy = "department")
    private DepartmentHead departmentHead;
    
    @OneToMany(mappedBy = "department")
    private Set<Course> courses = new HashSet<>();
    
    @OneToMany(mappedBy = "department")
    private Set<Instructor> instructors = new HashSet<>();
}