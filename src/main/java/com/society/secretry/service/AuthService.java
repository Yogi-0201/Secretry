package com.society.secretry.service;

import com.society.secretry.model.Flat;
import com.society.secretry.repository.FlatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private FlatRepository flatRepository;

    private final Map<String, SessionUser> tokenSessionMap = new ConcurrentHashMap<>();

    public static class SessionUser {
        private String username;
        private String role; // "ADMIN" or "RESIDENT"
        private String email;
        private String flatNumber;

        public SessionUser(String username, String role, String email, String flatNumber) {
            this.username = username;
            this.role = role;
            this.email = email;
            this.flatNumber = flatNumber;
        }

        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
        public String getFlatNumber() { return flatNumber; }
    }

    public Optional<String> login(String loginId, String password, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            if ("admin".equalsIgnoreCase(loginId) && "admin123".equals(password)) {
                String token = UUID.randomUUID().toString();
                tokenSessionMap.put(token, new SessionUser("Admin Secretary", "ADMIN", "admin@society.com", "ADMIN"));
                return Optional.of(token);
            }
            return Optional.empty();
        } else {
            // Resident login - loginId can be flatNumber or email
            Optional<Flat> flatOpt = flatRepository.findByFlatNumber(loginId);
            if (flatOpt.isEmpty()) {
                flatOpt = flatRepository.findByEmail(loginId);
            }

            if (flatOpt.isPresent() && flatOpt.get().getPassword().equals(password)) {
                Flat flat = flatOpt.get();
                String token = UUID.randomUUID().toString();
                tokenSessionMap.put(token, new SessionUser(flat.getOwnerName(), "RESIDENT", flat.getEmail(), flat.getFlatNumber()));
                return Optional.of(token);
            }
            return Optional.empty();
        }
    }

    public Optional<SessionUser> validateToken(String token) {
        if (token == null) return Optional.empty();
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        return Optional.ofNullable(tokenSessionMap.get(token));
    }

    public void logout(String token) {
        if (token == null) return;
        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }
        tokenSessionMap.remove(token);
    }
}
