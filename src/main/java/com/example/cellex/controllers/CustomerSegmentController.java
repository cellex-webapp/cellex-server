package com.example.cellex.controllers;

import com.example.cellex.dtos.request.segment.CreateCustomerSegmentRequest;
import com.example.cellex.dtos.request.segment.UpdateCustomerSegmentRequest;
import com.example.cellex.dtos.response.segment.CustomerSegmentResponse;
import com.example.cellex.services.segment.CustomerSegmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer-segments")
@RequiredArgsConstructor
@Tag(name = "04. Customer Segments", description = "API quản lý phân khúc khách hàng")
public class CustomerSegmentController {

    private final CustomerSegmentService customerSegmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo phân khúc khách hàng mới", description = "Chỉ ADMIN có thể tạo")
    public ResponseEntity<CustomerSegmentResponse> createSegment(
            @Valid @RequestBody CreateCustomerSegmentRequest request
    ) {
        return ResponseEntity.ok(customerSegmentService.createSegment(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật phân khúc khách hàng", description = "Chỉ ADMIN có thể cập nhật")
    public ResponseEntity<CustomerSegmentResponse> updateSegment(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerSegmentRequest request
    ) {
        return ResponseEntity.ok(customerSegmentService.updateSegment(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa phân khúc khách hàng", description = "Chỉ ADMIN có thể xóa")
    public ResponseEntity<Void> deleteSegment(@PathVariable String id) {
        customerSegmentService.deleteSegment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin phân khúc theo ID")
    public ResponseEntity<CustomerSegmentResponse> getSegmentById(@PathVariable String id) {
        return ResponseEntity.ok(customerSegmentService.getSegmentById(id));
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả phân khúc khách hàng")
    public ResponseEntity<List<CustomerSegmentResponse>> getAllSegments() {
        return ResponseEntity.ok(customerSegmentService.getAllSegments());
    }
}

