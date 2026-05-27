package com.society.secretry.service;

import com.society.secretry.model.Payment;
import com.society.secretry.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FlatService flatService;

    @Transactional
    public Payment recordPayment(String flatNumber, Double amount, String description) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        // Validate flat exists
        flatService.getFlatByFlatNumber(flatNumber)
                .orElseThrow(() -> new RuntimeException("Flat number not found in society database: " + flatNumber));

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = new Payment(flatNumber, amount, transactionId, description);
        
        Payment savedPayment = paymentRepository.save(payment);
        flatService.reduceMaintenanceDue(flatNumber, amount);

        return savedPayment;
    }

    public List<Payment> getPaymentHistory(String flatNumber) {
        return paymentRepository.findByFlatNumberOrderByPaymentDateDesc(flatNumber);
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }
}
