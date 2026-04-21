package com.noaats.cost.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a Pageable from request parameters.
 * Used by controllers to support server-side pagination + sorting.
 */
public final class PageHelper {

    private PageHelper() {}

    /**
     * Build a Pageable from the given parameters.
     *
     * @param page      0-based page number
     * @param size      page size (default 20, max 200)
     * @param sortBy    property to sort by (supports nested like "department.name")
     * @param sortDir   "asc" or "desc"
     * @return a PageRequest
     */
    public static Pageable of(int page, int size, String sortBy, String sortDir) {
        size = Math.max(1, Math.min(size, 200));
        Sort sort = Sort.unsorted();
        if (sortBy != null && !sortBy.isBlank()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(dir, sortBy);
        }
        return PageRequest.of(page, size, sort);
    }
}
