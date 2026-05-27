package com.society.secretry.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_logs")
public class VisitorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_phone", nullable = false)
    private String guestPhone;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime = LocalDateTime.now();

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    public VisitorLog() {}

    public VisitorLog(String guestName, String guestPhone, String flatNumber, String purpose) {
        this.guestName = guestName;
        this.guestPhone = guestPhone;
        this.flatNumber = flatNumber;
        this.purpose = purpose;
        this.checkInTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestPhone() { return guestPhone; }
    public void setGuestPhone(String guestPhone) { this.guestPhone = guestPhone; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
}
