package com.society.secretry.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "poll_votes",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"poll_id", "flat_number"})
    }
)
public class PollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "selected_option", nullable = false)
    private String selectedOption;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt = LocalDateTime.now();

    public PollVote() {}

    public PollVote(Long pollId, String flatNumber, String selectedOption) {
        this.pollId = pollId;
        this.flatNumber = flatNumber;
        this.selectedOption = selectedOption;
        this.votedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPollId() { return pollId; }
    public void setPollId(Long pollId) { this.pollId = pollId; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
