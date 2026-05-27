package com.society.secretry.repository;

import com.society.secretry.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByFlatNumberOrderByPaymentDateDesc(String flatNumber);
    List<Payment> findAllByOrderByPaymentDateDesc();
}
