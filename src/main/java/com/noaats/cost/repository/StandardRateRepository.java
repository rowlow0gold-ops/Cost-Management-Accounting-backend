package com.noaats.cost.repository;

import com.noaats.cost.domain.StandardRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StandardRateRepository extends JpaRepository<StandardRate, Long> {
    List<StandardRate> findByYearMonth(String yearMonth);
    Optional<StandardRate> findByYearMonthAndGrade(String yearMonth, String grade);
}
