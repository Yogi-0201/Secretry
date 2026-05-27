package com.society.secretry.repository;

import com.society.secretry.model.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {
    Optional<PollVote> findByPollIdAndFlatNumber(Long pollId, String flatNumber);
    List<PollVote> findByPollId(Long pollId);
    long countByPollId(Long pollId);
}
