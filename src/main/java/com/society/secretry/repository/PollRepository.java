package com.society.secretry.repository;

import com.society.secretry.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PollRepository extends JpaRepository<Poll, Long> {
    List<Poll> findAllByOrderByCreatedAtDesc();
}
