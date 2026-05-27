package com.society.secretry.controller;

import com.society.secretry.model.Payment;
import com.society.secretry.service.AuthService;
import com.society.secretry.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AuthService authService;

    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized. Please log in first."));
        }

        AuthService.SessionUser user = userOpt.get();
        if ("ADMIN".equals(user.getRole())) {
            return ResponseEntity.ok(paymentService.getAllPayments());
        } else {
            return ResponseEntity.ok(paymentService.getPaymentHistory(user.getFlatNumber()));
        }
    }

    @PostMapping("/pay")
    public ResponseEntity<?> payMaintenance(@RequestBody Map<String, Object> payload, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Optional<AuthService.SessionUser> userOpt = authService.validateToken(authHeader);
        if (userOpt.isEmpty() || !"RESIDENT".equals(userOpt.get().getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only society residents can pay maintenance online."));
        }

        AuthService.SessionUser user = userOpt.get();
        Object amtObj = payload.get("amount");
        String description = (String) payload.get("description");

        if (amtObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Payment amount is required."));
        }

        double amount;
        try {
            amount = Double.parseDouble(amtObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid payment amount format."));
        }

        try {
            Payment receipt = paymentService.recordPayment(user.getFlatNumber(), amount, description);
            return ResponseEntity.ok(receipt);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
