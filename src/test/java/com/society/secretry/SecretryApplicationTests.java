package com.society.secretry;

import com.society.secretry.model.*;
import com.society.secretry.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SecretryApplicationTests {

    @Autowired
    private FlatService flatService;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private PollService pollService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void contextLoads() {
        // Simple context validation
    }

    @Test
    void testFlatManagement() {
        Flat flat = new Flat("X-999", "Bruce Wayne", "bruce@wayne.com", "batman123", "+1 12345678", 1, 10000.0);
        Flat saved = flatService.saveFlat(flat);
        
        assertNotNull(saved.getId());
        assertEquals("X-999", saved.getFlatNumber());
        assertEquals("Bruce Wayne", saved.getOwnerName());
        
        Optional<Flat> fetched = flatService.getFlatByFlatNumber("X-999");
        assertTrue(fetched.isPresent());
        assertEquals(10000.0, fetched.get().getMaintenanceBalance());
    }

    @Test
    void testVisitorCheckinAndCheckout() {
        VisitorLog log = visitorService.checkIn("Clark Kent", "+1 888777666", "A-101", "Daily Planet Interview");
        assertNotNull(log.getId());
        assertNull(log.getCheckOutTime());
        
        // Checkout
        VisitorLog updated = visitorService.checkOut(log.getId());
        assertNotNull(updated.getCheckOutTime());
    }

    @Test
    void testUniqueVotingSystem() {
        Poll poll = pollService.createPoll("Paint the exterior walls?", "Deciding the next coloring theme", "Indigo,Grey,White", 3);
        assertNotNull(poll.getId());
        
        // Cast unique vote
        PollVote vote = pollService.castVote(poll.getId(), "A-101", "Indigo");
        assertNotNull(vote.getId());
        assertEquals("Indigo", vote.getSelectedOption());
        
        // Try casting vote again from same flat (should fail!)
        Exception exception = assertThrows(RuntimeException.class, () -> {
            pollService.castVote(poll.getId(), "A-101", "Grey");
        });
        
        assertTrue(exception.getMessage().contains("already cast a vote"));
    }

    @Test
    void testMaintenancePaymentProcessing() {
        // Setup a temporary flat
        Flat flat = new Flat("Z-505", "Peter Parker", "peter@dailybugle.com", "spidey12", "+1 987654", 2, 5000.0);
        flatService.saveFlat(flat);
        
        // Pay dues
        Payment receipt = paymentService.recordPayment("Z-505", 2000.0, "Monthly partial pay");
        assertNotNull(receipt.getId());
        assertNotNull(receipt.getTransactionId());
        
        // Verify balance decreased from 5000 to 3000
        Optional<Flat> updatedFlat = flatService.getFlatByFlatNumber("Z-505");
        assertTrue(updatedFlat.isPresent());
        assertEquals(3000.0, updatedFlat.get().getMaintenanceBalance());
    }
}
