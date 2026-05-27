package com.society.secretry.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "polls")
public class Poll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String options; // Comma separated options, e.g. "Yes,No,Abstain"

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE, CLOSED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Poll() {}

    public Poll(String question, String description, String options, LocalDateTime expiresAt) {
        this.question = question;
        this.description = description;
        this.options = options;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
