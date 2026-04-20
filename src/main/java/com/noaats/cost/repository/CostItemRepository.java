package com.noaats.cost.repository;

import com.noaats.cost.domain.CostItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostItemRepository extends JpaRepository<CostItem, Long> {
    List<CostItem> findByYearMonth(String yearMonth);
    List<CostItem> findByYearMonthAndType(String yearMonth, CostItem.CostType type);
}
