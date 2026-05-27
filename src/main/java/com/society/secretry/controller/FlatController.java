package com.society.secretry.controller;

import com.society.secretry.model.Flat;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.FlatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/flats")
@CrossOrigin(origins = "*")
public class FlatController {

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
    public ResponseEntity<?> getAllFlats(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        return ResponseEntity.ok(flatService.getAllFlats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFlatById(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        return flatService.getFlatById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createFlat(@RequestBody Flat flat, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        
        // Validate flat number uniqueness
        if (flatService.getFlatByFlatNumber(flat.getFlatNumber()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Flat number already exists in database."));
        }

        try {
            Flat savedFlat = flatService.saveFlat(flat);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFlat);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFlat(@PathVariable Long id, @RequestBody Flat flat, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            Flat updatedFlat = flatService.updateFlat(id, flat);
            return ResponseEntity.ok(updatedFlat);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFlat(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            flatService.deleteFlat(id);
            return ResponseEntity.ok(Map.of("message", "Flat deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // Advanced: Charge monthly maintenance or one-off penalty
    @PostMapping("/charge")
    public ResponseEntity<?> chargeMaintenance(@RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }

        String flatNumber = (String) payload.get("flatNumber"); // can be "ALL" or specific e.g. "A-101"
        double amount = Double.parseDouble(payload.get("amount").toString());

        if (amount <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Charge amount must be greater than zero."));
        }

        if ("ALL".equalsIgnoreCase(flatNumber)) {
            List<Flat> flats = flatService.getAllFlats();
            for (Flat f : flats) {
                flatService.addMaintenanceDue(f.getFlatNumber(), amount);
            }
            return ResponseEntity.ok(Map.of("message", "Charged " + amount + " to all " + flats.size() + " flats successfully."));
        } else {
            Optional<Flat> flatOpt = flatService.getFlatByFlatNumber(flatNumber);
            if (flatOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Flat number " + flatNumber + " not found."));
            }
            flatService.addMaintenanceDue(flatNumber, amount);
            return ResponseEntity.ok(Map.of("message", "Charged " + amount + " to flat " + flatNumber + " successfully."));
        }
    }
}
