package com.example.cellex.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Base Pagination Response
 * Sử dụng cho tất cả API cần phân trang
 *
 * @param <T> Kiểu dữ liệu của items
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {


    private List<T> content;


    private Integer currentPage;

    private Integer pageSize;

    private Long totalElements;

    private Integer totalPages;

    private Boolean hasPrevious;

    private Boolean hasNext;

    private Boolean isFirst;

    private Boolean isLast;

    private Boolean isEmpty;

    /**
     * Thông tin sắp xếp (nếu có)
     */
    private SortInfo sort;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SortInfo {
        /**
         * Có sắp xếp không
         */
        private Boolean sorted;

        /**
         * Trường sắp xếp
         */
        private String sortBy;

        /**
         * Hướng sắp xếp (ASC/DESC)
         */
        private String direction;
    }

    /**
     * Tạo PageResponse từ Spring Data Page
     *
     * @param page Spring Data Page object
     * @param <T> Kiểu dữ liệu
     * @return PageResponse
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        PageResponseBuilder<T> builder = PageResponse.<T>builder()
                .content(page.getContent())
                .currentPage(page.getNumber() + 1) // Spring Page bắt đầu từ 0, chuyển thành 1
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasPrevious(page.hasPrevious())
                .hasNext(page.hasNext())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .isEmpty(page.isEmpty());

        // Thêm thông tin sort nếu có
        if (page.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = page.getSort().iterator().next();
            builder.sort(SortInfo.builder()
                    .sorted(true)
                    .sortBy(order.getProperty())
                    .direction(order.getDirection().name())
                    .build());
        } else {
            builder.sort(SortInfo.builder()
                    .sorted(false)
                    .build());
        }

        return builder.build();
    }

    /**
     * Tạo PageResponse từ Spring Data Page với custom mapper
     *
     * @param page Spring Data Page object
     * @param mapper Function để map từ entity sang DTO
     * @param <T> Kiểu dữ liệu đầu ra
     * @param <E> Kiểu dữ liệu đầu vào
     * @return PageResponse
     */
    public static <T, E> PageResponse<T> of(
            org.springframework.data.domain.Page<E> page,
            java.util.function.Function<E, T> mapper) {

        List<T> mappedContent = page.getContent().stream()
                .map(mapper)
                .collect(java.util.stream.Collectors.toList());

        PageResponseBuilder<T> builder = PageResponse.<T>builder()
                .content(mappedContent)
                .currentPage(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasPrevious(page.hasPrevious())
                .hasNext(page.hasNext())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .isEmpty(page.isEmpty());

        // Thêm thông tin sort nếu có
        if (page.getSort().isSorted()) {
            org.springframework.data.domain.Sort.Order order = page.getSort().iterator().next();
            builder.sort(SortInfo.builder()
                    .sorted(true)
                    .sortBy(order.getProperty())
                    .direction(order.getDirection().name())
                    .build());
        } else {
            builder.sort(SortInfo.builder()
                    .sorted(false)
                    .build());
        }

        return builder.build();
    }
}

