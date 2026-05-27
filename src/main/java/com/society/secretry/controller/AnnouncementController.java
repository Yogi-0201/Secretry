package com.society.secretry.controller;

import com.society.secretry.model.Announcement;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

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
    public ResponseEntity<?> getAllAnnouncements(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAuthorized(authHeader)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized. Please log in first."));
        }
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @PostMapping
    public ResponseEntity<?> createAnnouncement(@RequestBody Announcement announcement, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            Announcement saved = announcementService.saveAnnouncement(announcement);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
