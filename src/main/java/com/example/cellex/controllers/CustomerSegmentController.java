package com.example.cellex.controllers;

import com.example.cellex.dtos.request.segment.CreateCustomerSegmentRequest;
import com.example.cellex.dtos.request.segment.UpdateCustomerSegmentRequest;
import com.example.cellex.dtos.response.ApiResponse;
import com.example.cellex.dtos.response.segment.CustomerSegmentResponse;
import com.example.cellex.services.segment.CustomerSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer-segments")
@RequiredArgsConstructor
@Tag(name = "04. Customer Segments", description = "API quản lý phân khúc khách hàng")
public class CustomerSegmentController {

    private final CustomerSegmentService customerSegmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo phân khúc khách hàng mới", description = "Chỉ ADMIN có thể tạo")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> createSegment(
            @Valid @RequestBody CreateCustomerSegmentRequest request
    ) {
        CustomerSegmentResponse segment = customerSegmentService.createSegment(request);
        ApiResponse<CustomerSegmentResponse> response = ApiResponse.<CustomerSegmentResponse>builder()
                .code(200)
                .message("Tạo phân khúc khách hàng thành công")
                .result(segment)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật phân khúc khách hàng", description = "Chỉ ADMIN có thể cập nhật")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> updateSegment(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerSegmentRequest request
    ) {
        CustomerSegmentResponse segment = customerSegmentService.updateSegment(id, request);
        ApiResponse<CustomerSegmentResponse> response = ApiResponse.<CustomerSegmentResponse>builder()
                .code(200)
                .message("Cập nhật phân khúc khách hàng thành công")
                .result(segment)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phân khúc khách hàng", description = "Chỉ ADMIN có thể xóa")
    public ResponseEntity<ApiResponse<String>> deleteSegment(@PathVariable String id) {
        customerSegmentService.deleteSegment(id);
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(200)
                .message("Xóa phân khúc khách hàng thành công")
                .result("Đã xóa")
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phân khúc theo ID")
    public ResponseEntity<ApiResponse<CustomerSegmentResponse>> getSegmentById(@PathVariable String id) {
        CustomerSegmentResponse segment = customerSegmentService.getSegmentById(id);
        ApiResponse<CustomerSegmentResponse> response = ApiResponse.<CustomerSegmentResponse>builder()
                .code(200)
                .message("Lấy thông tin phân khúc thành công")
                .result(segment)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả phân khúc khách hàng")
    public ResponseEntity<ApiResponse<com.example.cellex.dtos.response.PageResponse<CustomerSegmentResponse>>> getAllSegments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortType
    ) {
        int pageNumber = Math.max(page - 1, 0);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortType) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(direction, sortBy));

        Page<CustomerSegmentResponse> pageEntity = customerSegmentService.getAllSegments(pageable);
        com.example.cellex.dtos.response.PageResponse<CustomerSegmentResponse> pageResp = com.example.cellex.dtos.response.PageResponse.of(pageEntity);

        ApiResponse<com.example.cellex.dtos.response.PageResponse<CustomerSegmentResponse>> response = ApiResponse.<com.example.cellex.dtos.response.PageResponse<CustomerSegmentResponse>>builder()
                .code(200)
                .message("Lấy danh sách phân khúc khách hàng thành công")
                .result(pageResp)
                .build();
        return ResponseEntity.ok(response);
    }
}
