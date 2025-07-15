package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.Instructor;
import com.dersprogrami.automaticscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Long> {
    Optional<Instructor> findByUser(User user);
}