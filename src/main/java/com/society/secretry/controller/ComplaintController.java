package com.society.secretry.controller;

import com.society.secretry.model.Complaint;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private AuthService authService;

    @GetMapping
    public ResponseEntity<?> getComplaints(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized. Please log in."));
        }

        AuthService.SessionUser user = userOpt.get();
        if ("ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(complaintService.getAllComplaints());
        } else {
            return ResponseEntity.ok(complaintService.getComplaintsByFlat(user.getFlatNumber()));
        }
    }

    @PostMapping
    public ResponseEntity<?> raiseComplaint(@RequestBody Map<String, String> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty() || !"RESIDENT".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only residents can file complaints."));
        }

        String title = payload.get("title");
        String description = payload.get("description");

        if (title == null || title.trim().isEmpty() || description == null || description.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Title and description are required fields."));
        }

        String flatNumber = userOpt.get().getFlatNumber();
        try {
            Complaint complaint = complaintService.createComplaint(flatNumber, title, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(complaint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty() || !"ADMIN".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Only admin can update complaint status."));
        }

        String status = payload.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Status field is required."));
        }

        try {
            Complaint updated = complaintService.updateStatus(id, status);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComplaint(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized."));
        }

        if (!"ADMIN".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required to delete."));
        }

        try {
            complaintService.deleteComplaint(id);
            return ResponseEntity.ok(Map.of("message", "Complaint record deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
