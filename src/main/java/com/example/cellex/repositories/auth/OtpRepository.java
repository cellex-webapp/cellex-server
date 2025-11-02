package com.example.cellex.repositories.auth;

import com.example.cellex.models.auth.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {
    Optional<Otp> findByCodeAndEmail(String code, String email);
}