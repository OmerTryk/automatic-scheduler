package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.Schedule;
import com.dersprogrami.automaticscheduler.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByDepartment(Department department);
    Optional<Schedule> findByDepartmentAndIsActiveTrue(Department department);
}