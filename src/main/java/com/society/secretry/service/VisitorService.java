package com.society.secretry.service;

import com.society.secretry.model.VisitorLog;
import com.society.secretry.repository.VisitorLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisitorService {

    @Autowired
    private VisitorLogRepository visitorLogRepository;

    public VisitorLog checkIn(String guestName, String guestPhone, String flatNumber, String purpose) {
        VisitorLog log = new VisitorLog(guestName, guestPhone, flatNumber, purpose);
        return visitorLogRepository.save(log);
    }

    public VisitorLog checkOut(Long id) {
        Optional<VisitorLog> logOpt = visitorLogRepository.findById(id);
        if (logOpt.isPresent()) {
            VisitorLog log = logOpt.get();
            if (log.getCheckOutTime() == null) {
                log.setCheckOutTime(LocalDateTime.now());
                return visitorLogRepository.save(log);
            }
            return log;
        }
        throw new RuntimeException("Visitor record not found with id: " + id);
    }

    public List<VisitorLog> getActiveVisitors() {
        return visitorLogRepository.findByCheckOutTimeIsNullOrderByCheckInTimeDesc();
    }

    public List<VisitorLog> getVisitorHistory() {
        return visitorLogRepository.findAllByOrderByCheckInTimeDesc();
    }
}
