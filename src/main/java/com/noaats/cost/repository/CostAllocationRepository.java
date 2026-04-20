package com.noaats.cost.repository;

import com.noaats.cost.domain.CostAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostAllocationRepository extends JpaRepository<CostAllocation, Long> {
    List<CostAllocation> findByYearMonth(String yearMonth);
    List<CostAllocation> findByYearMonthAndKind(String yearMonth, CostAllocation.AllocationKind kind);
    void deleteByYearMonthAndKind(String yearMonth, CostAllocation.AllocationKind kind);
}
