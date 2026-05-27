package com.society.secretry;

import com.society.secretry.model.*;
import com.society.secretry.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FlatRepository flatRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private PollVoteRepository pollVoteRepository;

    @Autowired
    private VisitorLogRepository visitorLogRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public void run(String... args) throws Exception {
        // If flats database is empty, seed initial data for demonstration
        if (flatRepository.count() == 0) {
            System.out.println(">>> Society database is empty. Seeding initial demonstration data...");

            // 1. Seed Flats/Residents
            Flat f1 = new Flat("A-101", "John Doe", "john@gmail.com", "password123", "+91 9876543210", 4, 2500.0);
            Flat f2 = new Flat("B-202", "Alice Smith", "alice@gmail.com", "password123", "+91 9999888877", 2, 0.0);
            Flat f3 = new Flat("C-303", "Bob Johnson", "bob@gmail.com", "password123", "+91 8888777766", 5, 4500.0);

            flatRepository.save(f1);
            flatRepository.save(f2);
            flatRepository.save(f3);

            // 2. Seed Announcements
            Announcement a1 = new Announcement(
                    "Water Supply Disruption Notice",
                    "Please note that the municipal water connection will be closed for maintenance this Thursday from 2:00 PM to 5:00 PM. Please store water in advance.",
                    "WARNING"
            );
            Announcement a2 = new Announcement(
                    "Annual General Body Meeting",
                    "Dear Residents, the AGM is scheduled for this Sunday at 10:00 AM in the society clubhouse. Important budget approvals are on the agenda. Please attend without fail.",
                    "GENERAL"
            );
            Announcement a3 = new Announcement(
                    "Diwali Celebration Planning Committee",
                    "Let's gather in the central courtyard at 7:00 PM on Friday for light planning, music, and delicious sweets. Happy festival season!",
                    "FESTIVAL"
            );

            announcementRepository.save(a1);
            announcementRepository.save(a2);
            announcementRepository.save(a3);

            // 3. Seed Complaints
            Complaint c1 = new Complaint("A-101", "Broken entrance stairs", "The third step of the entrance stairs of Wing A is cracked and loose. It is a severe slipping hazard for senior citizens.");
            c1.setStatus("IN_PROGRESS");
            complaintRepository.save(c1);

            Complaint c2 = new Complaint("C-303", "Elevator fan malfunctioning", "The ceiling fan in Wing B Elevator #1 is making grinding noises and not running at full speed. It is extremely stuffy inside.");
            complaintRepository.save(c2);

            // 4. Seed Polls
            Poll p1 = new Poll(
                    "Should we install Solar Panels on the clubhouse terrace?",
                    "We plan to install solar panels to power all passage lights and the water pumps. The estimated setup cost is $5,000, which will reduce our monthly common electricity bill by 30%. It is eco-friendly and cost-effective.",
                    "Yes (Highly Recommended),No,Need more discussion",
                    LocalDateTime.now().plusDays(7)
            );
            pollRepository.save(p1);

            PollVote vote1 = new PollVote(p1.getId(), "A-101", "Yes (Highly Recommended)");
            pollVoteRepository.save(vote1);

            // 5. Seed GuestLogs
            VisitorLog v1 = new VisitorLog("Richard Roe", "+91 9123456789", "B-202", "Courier Delivery");
            visitorLogRepository.save(v1);

            VisitorLog v2 = new VisitorLog("Emma Watson", "+91 9456781230", "A-101", "Family Visit");
            v2.setCheckOutTime(LocalDateTime.now().minusHours(1));
            visitorLogRepository.save(v2);

            // 6. Seed Payments
            Payment pay1 = new Payment("A-101", 1500.0, "TXN-DEMO123", "Partial Maintenance Payment");
            paymentRepository.save(pay1);

            System.out.println(">>> Demo data seeded successfully!");
        }
    }
}
