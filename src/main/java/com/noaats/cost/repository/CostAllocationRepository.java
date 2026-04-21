package com.noaats.cost.repository;

import com.noaats.cost.domain.CostAllocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CostAllocationRepository extends JpaRepository<CostAllocation, Long> {
    List<CostAllocation> findByYearMonth(String yearMonth);
    List<CostAllocation> findByYearMonthAndKind(String yearMonth, CostAllocation.AllocationKind kind);
    void deleteByYearMonthAndKind(String yearMonth, CostAllocation.AllocationKind kind);

    @Query("SELECT a FROM CostAllocation a " +
           "LEFT JOIN a.sourceDepartment sd LEFT JOIN a.targetProject tp WHERE " +
           "a.yearMonth = :yearMonth AND a.kind = 'STANDARD_ALLOC' AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(sd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(tp.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(a.basis AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.memo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CostAllocation> searchAllocations(@Param("yearMonth") String yearMonth,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);

    @Query("SELECT a FROM CostAllocation a " +
           "LEFT JOIN a.sourceDepartment sd LEFT JOIN a.targetDepartment td WHERE " +
           "a.yearMonth = :yearMonth AND a.kind = 'TRANSFER' AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(sd.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(td.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.memo) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CostAllocation> searchTransfers(@Param("yearMonth") String yearMonth,
                                          @Param("keyword") String keyword,
                                          Pageable pageable);
}
