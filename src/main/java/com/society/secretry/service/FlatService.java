package com.society.secretry.service;

import com.society.secretry.model.Flat;
import com.society.secretry.repository.FlatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlatService {

    @Autowired
    private FlatRepository flatRepository;

    public List<Flat> getAllFlats() {
        return flatRepository.findAll();
    }

    public Optional<Flat> getFlatById(Long id) {
        return flatRepository.findById(id);
    }

    public Optional<Flat> getFlatByFlatNumber(String flatNumber) {
        return flatRepository.findByFlatNumber(flatNumber);
    }

    public Flat saveFlat(Flat flat) {
        // Enforce safe defaults if null
        if (flat.getMemberCount() == null) flat.setMemberCount(1);
        if (flat.getMaintenanceBalance() == null) flat.setMaintenanceBalance(0.0);
        return flatRepository.save(flat);
    }

    public Flat updateFlat(Long id, Flat updatedFlat) {
        return flatRepository.findById(id).map(flat -> {
            flat.setFlatNumber(updatedFlat.getFlatNumber());
            flat.setOwnerName(updatedFlat.getOwnerName());
            flat.setEmail(updatedFlat.getEmail());
            if (updatedFlat.getPassword() != null && !updatedFlat.getPassword().trim().isEmpty()) {
                flat.setPassword(updatedFlat.getPassword());
            }
            flat.setPhoneNumber(updatedFlat.getPhoneNumber());
            flat.setMemberCount(updatedFlat.getMemberCount() != null ? updatedFlat.getMemberCount() : flat.getMemberCount());
            flat.setMaintenanceBalance(updatedFlat.getMaintenanceBalance() != null ? updatedFlat.getMaintenanceBalance() : flat.getMaintenanceBalance());
            return flatRepository.save(flat);
        }).orElseThrow(() -> new RuntimeException("Flat not found with id: " + id));
    }

    public void deleteFlat(Long id) {
        flatRepository.deleteById(id);
    }

    public void addMaintenanceDue(String flatNumber, double amount) {
        flatRepository.findByFlatNumber(flatNumber).ifPresent(flat -> {
            flat.setMaintenanceBalance(flat.getMaintenanceBalance() + amount);
            flatRepository.save(flat);
        });
    }

    public void reduceMaintenanceDue(String flatNumber, double amount) {
        flatRepository.findByFlatNumber(flatNumber).ifPresent(flat -> {
            flat.setMaintenanceBalance(Math.max(0.0, flat.getMaintenanceBalance() - amount));
            flatRepository.save(flat);
        });
    }
}
