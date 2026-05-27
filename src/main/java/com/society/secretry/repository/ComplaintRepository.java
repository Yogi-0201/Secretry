package com.society.secretry.repository;

import com.society.secretry.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByFlatNumberOrderByCreatedAtDesc(String flatNumber);
    List<Complaint> findAllByOrderByCreatedAtDesc();
}
