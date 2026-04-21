package com.noaats.cost.repository;

import com.noaats.cost.domain.CostItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CostItemRepository extends JpaRepository<CostItem, Long> {
    List<CostItem> findByYearMonth(String yearMonth);
    List<CostItem> findByYearMonthAndType(String yearMonth, CostItem.CostType type);

    @Query("SELECT c FROM CostItem c LEFT JOIN c.department d WHERE " +
           "c.yearMonth = :yearMonth AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(c.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(CAST(c.type AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<CostItem> search(@Param("yearMonth") String yearMonth,
                          @Param("keyword") String keyword,
                          Pageable pageable);
}
