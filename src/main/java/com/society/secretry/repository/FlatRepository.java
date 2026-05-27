package com.society.secretry.repository;

import com.society.secretry.model.Flat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface FlatRepository extends JpaRepository<Flat, Long> {
    Optional<Flat> findByFlatNumber(String flatNumber);
    Optional<Flat> findByEmail(String email);
}
