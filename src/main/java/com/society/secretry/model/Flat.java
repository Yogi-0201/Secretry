package com.society.secretry.model;

import jakarta.persistence.*;

@Entity
@Table(name = "flats")
public class Flat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number", unique = true, nullable = false)
    private String flatNumber;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "member_count")
    private Integer memberCount = 1;

    @Column(name = "maintenance_balance")
    private Double maintenanceBalance = 0.0;

    public Flat() {}

    public Flat(String flatNumber, String ownerName, String email, String password, String phoneNumber, Integer memberCount, Double maintenanceBalance) {
        this.flatNumber = flatNumber;
        this.ownerName = ownerName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.memberCount = memberCount;
        this.maintenanceBalance = maintenanceBalance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlatNumber() { return flatNumber; }
    public void setFlatNumber(String flatNumber) { this.flatNumber = flatNumber; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }
    public Double getMaintenanceBalance() { return maintenanceBalance; }
    public void setMaintenanceBalance(Double maintenanceBalance) { this.maintenanceBalance = maintenanceBalance; }
}
