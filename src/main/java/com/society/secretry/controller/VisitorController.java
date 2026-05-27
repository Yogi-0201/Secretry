package com.society.secretry.controller;

import com.society.secretry.model.VisitorLog;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.VisitorService;
import com.society.secretry.service.FlatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/visitors")
@CrossOrigin(origins = "*")
public class VisitorController {

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private FlatService flatService;

    @Autowired
    private AuthService authService;

    private boolean isAdmin(String authHeader) {
        return authService.validateToken(authHeader)
                .map(user -> "ADMIN".equals(user.getRole()))
                .orElse(false);
    }

    @GetMapping
    public ResponseEntity<?> getVisitors(
            @RequestParam(value = "active", defaultValue = "false") boolean activeOnly,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        if (activeOnly) {
            return ResponseEntity.ok(visitorService.getActiveVisitors());
        }
        return ResponseEntity.ok(visitorService.getVisitorHistory());
    }

    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, String> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }

        String guestName = payload.get("guestName");
        String guestPhone = payload.get("guestPhone");
        String flatNumber = payload.get("flatNumber");
        String purpose = payload.get("purpose");

        if (guestName == null || guestName.trim().isEmpty() ||
            guestPhone == null || guestPhone.trim().isEmpty() ||
            flatNumber == null || flatNumber.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Guest Name, Phone, and Flat Number are required."));
        }

        // Validate flat number exists before recording visitor
        if (flatService.getFlatByFlatNumber(flatNumber).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Flat number " + flatNumber + " does not exist in society database."));
        }

        try {
            VisitorLog log = visitorService.checkIn(guestName, guestPhone, flatNumber, purpose);
            return ResponseEntity.status(HttpStatus.CREATED).body(log);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<?> checkOut(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            VisitorLog updated = visitorService.checkOut(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
