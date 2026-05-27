package com.society.secretry.controller;

import com.society.secretry.model.Flat;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.FlatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FlatService flatService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String loginId = payload.get("loginId");
        String password = payload.get("password");
        String role = payload.get("role");

        if (loginId == null || password == null || role == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Missing loginId, password or role."));
        }

        Optional<String> tokenOpt = authService.login(loginId, password, role);
        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            Optional<AuthService.SessionUser> userOpt = authService.validateToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            userOpt.ifPresent(user -> {
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("email", user.getEmail());
                response.put("flatNumber", user.getFlatNumber());
            });
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials. Please check your login details and role."));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> sessionUserOpt = authService.validateToken(authHeader);
        if (sessionUserOpt.isPresent()) {
            AuthService.SessionUser user = sessionUserOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("email", user.getEmail());
            response.put("flatNumber", user.getFlatNumber());

            // If resident, enrich with current flat details (e.g. balance, members count)
            if ("RESIDENT".equals(user.getRole())) {
                Optional<Flat> flatOpt = flatService.getFlatByFlatNumber(user.getFlatNumber());
                flatOpt.ifPresent(flat -> {
                    response.put("maintenanceBalance", flat.getMaintenanceBalance());
                    response.put("memberCount", flat.getMemberCount());
                    response.put("phoneNumber", flat.getPhoneNumber());
                    response.put("ownerName", flat.getOwnerName());
                });
            }
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Session expired or invalid token. Please log in again."));
    }
}
