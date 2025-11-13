package com.example.cellex.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class để tạo Pageable request
 * Giúp standardize pagination logic across controllers
 */
public class PaginationUtil {

    /**
     * Tạo Pageable từ các tham số pagination
     *
     * @param page Số trang (1-based, bắt đầu từ 1)
     * @param limit Số lượng items mỗi trang
     * @param sortBy Trường để sắp xếp
     * @param sortType Hướng sắp xếp (asc/desc)
     * @return Pageable object
     */
    public static Pageable createPageable(Integer page, Integer limit, String sortBy, String sortType) {
        // Chuyển từ 1-based sang 0-based
        int pageNumber = Math.max(page != null ? page - 1 : 0, 0);
        int pageSize = limit != null && limit > 0 ? limit : 10;

        // Xác định hướng sắp xếp
        Sort.Direction direction = sortType != null && "asc".equalsIgnoreCase(sortType)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        String sortField = sortBy != null && !sortBy.isEmpty() ? sortBy : "createdAt";

        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortField));
    }

    /**
     * Tạo Pageable với default sort by createdAt desc
     */
    public static Pageable createPageable(Integer page, Integer limit) {
        return createPageable(page, limit, "createdAt", "desc");
    }
}

