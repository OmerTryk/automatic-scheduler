package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.Course;
import com.dersprogrami.automaticscheduler.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByDepartment(Department department);
    Optional<Course> findByCodeAndDepartment(String code, Department department);
}