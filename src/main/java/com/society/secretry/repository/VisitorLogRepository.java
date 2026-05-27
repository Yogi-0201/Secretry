package com.society.secretry.repository;

import com.society.secretry.model.VisitorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VisitorLogRepository extends JpaRepository<VisitorLog, Long> {
    List<VisitorLog> findByCheckOutTimeIsNullOrderByCheckInTimeDesc();
    List<VisitorLog> findAllByOrderByCheckInTimeDesc();
}
