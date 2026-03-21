package com.aalap.aalapbackend.repository;

import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoolRepository extends JpaRepository<Nool, Long> {
    List<Nool> findByCreatedBy(User createdBy);
}
