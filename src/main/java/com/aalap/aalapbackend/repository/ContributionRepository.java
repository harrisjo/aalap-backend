package com.aalap.aalapbackend.repository;

import com.aalap.aalapbackend.entity.Contribution;
import com.aalap.aalapbackend.entity.Nool;
import com.aalap.aalapbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ContributionRepository extends JpaRepository<Contribution,Long> {
    List<Contribution> findByNool(Nool nool);
    List<Contribution> findByUser(User user);

    @Query("SELECT c FROM Contribution c WHERE c.nool IN :nools")
    List<Contribution> findByNoolIn(@Param("nools") List<Nool> nools);
}
