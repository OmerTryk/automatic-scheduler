package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.DepartmentHead;
import com.dersprogrami.automaticscheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartmentHeadRepository extends JpaRepository<DepartmentHead, Long> {
    Optional<DepartmentHead> findByUser(User user);
}