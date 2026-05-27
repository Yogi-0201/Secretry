package com.society.secretry.service;

import com.society.secretry.model.Complaint;
import com.society.secretry.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    public Complaint createComplaint(String flatNumber, String title, String description) {
        Complaint complaint = new Complaint(flatNumber, title, description);
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> getComplaintsByFlat(String flatNumber) {
        return complaintRepository.findByFlatNumberOrderByCreatedAtDesc(flatNumber);
    }

    public Complaint updateStatus(Long id, String status) {
        return complaintRepository.findById(id).map(complaint -> {
            complaint.setStatus(status.toUpperCase());
            return complaintRepository.save(complaint);
        }).orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));
    }

    public void deleteComplaint(Long id) {
        complaintRepository.deleteById(id);
    }
}
