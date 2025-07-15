package com.dersprogrami.automaticscheduler.repository;

import com.dersprogrami.automaticscheduler.model.ScheduleItem;
import com.dersprogrami.automaticscheduler.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {
    List<ScheduleItem> findBySchedule(Schedule schedule);
}