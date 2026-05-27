package com.society.secretry.controller;

import com.society.secretry.model.Poll;
import com.society.secretry.model.PollVote;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.PollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/polls")
@CrossOrigin(origins = "*")
public class PollController {

    @Autowired
    private PollService pollService;

    @Autowired
    private AuthService authService;

    private boolean isAuthorized(String authHeader) {
        return authService.validateToken(authHeader).isPresent();
    }

    private boolean isAdmin(String authHeader) {
        return authService.validateToken(authHeader)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }

    @GetMapping
    public ResponseEntity<?> getAllPolls(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized. Please log in first."));
        }
        return ResponseEntity.ok(pollService.getAllPolls());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPollDetails(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized."));
        }

        String userFlat = "RESIDENT".equals(userOpt.get().getRole()) ? userOpt.get().getFlatNumber() : null;
        try {
            PollService.PollDetails details = pollService.getPollDetails(id, userFlat);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPoll(@RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }

        String question = (String) payload.get("question");
        String description = (String) payload.get("description");
        String options = (String) payload.get("options"); // Comma-separated
        Integer durationDays = payload.get("durationDays") != null ? Integer.parseInt(payload.get("durationDays").toString()) : null;

        if (question == null || question.trim().isEmpty() || options == null || options.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Question and Options are required."));
        }

        try {
            Poll poll = pollService.createPoll(question, description, options, durationDays);
            return ResponseEntity.status(HttpStatus.CREATED).body(poll);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<?> castVote(@PathVariable Long id, @RequestBody Map<String, String> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty() || !"RESIDENT".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only society residents can vote."));
        }

        String selectedOption = payload.get("option");
        if (selectedOption == null || selectedOption.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Vote option must be specified."));
        }

        String flatNumber = userOpt.get().getFlatNumber();
        try {
            PollVote vote = pollService.castVote(id, flatNumber, selectedOption);
            return ResponseEntity.ok(vote);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> closePoll(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            Poll closedPoll = pollService.closePoll(id);
            return ResponseEntity.ok(closedPoll);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
