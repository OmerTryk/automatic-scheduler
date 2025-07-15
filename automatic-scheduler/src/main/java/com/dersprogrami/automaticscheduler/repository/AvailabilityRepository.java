package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.Availability;
import com.dersprogrami.automaticscheduler.model.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByInstructor(Instructor instructor);
    List<Availability> findByInstructorAndDayOfWeek(Instructor instructor, Availability.DayOfWeek dayOfWeek);
}