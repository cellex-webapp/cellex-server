package com.example.cellex.services;

import com.example.cellex.dtos.request.CreateCustomerSegmentRequest;
import com.example.cellex.dtos.request.UpdateCustomerSegmentRequest;
import com.example.cellex.dtos.response.CustomerSegmentResponse;
import com.example.cellex.exceptions.AppException;
import com.example.cellex.exceptions.ErrorCode;
import com.example.cellex.models.CustomerSegment;
import com.example.cellex.repositories.CustomerSegmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerSegmentService {

    private final CustomerSegmentRepository customerSegmentRepository;

    public CustomerSegmentResponse createSegment(CreateCustomerSegmentRequest request) {
        // Validate: maxSpend phải lớn hơn minSpend (nếu có)
        if (request.getMaxSpend() != null && request.getMaxSpend() <= request.getMinSpend()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "maxSpend phải lớn hơn minSpend");
        }

        CustomerSegment segment = CustomerSegment.builder()
                .name(request.getName())
                .minSpend(request.getMinSpend())
                .maxSpend(request.getMaxSpend())
                .level(request.getLevel())
                .description(request.getDescription())
                .build();

        segment = customerSegmentRepository.save(segment);
        return mapToResponse(segment);
    }

    public CustomerSegmentResponse updateSegment(String id, UpdateCustomerSegmentRequest request) {
        CustomerSegment segment = customerSegmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEGMENT_NOT_FOUND));

        if (request.getName() != null) {
            segment.setName(request.getName());
        }
        if (request.getMinSpend() != null) {
            segment.setMinSpend(request.getMinSpend());
        }
        if (request.getMaxSpend() != null) {
            segment.setMaxSpend(request.getMaxSpend());
        }
        if (request.getLevel() != null) {
            segment.setLevel(request.getLevel());
        }
        if (request.getDescription() != null) {
            segment.setDescription(request.getDescription());
        }

        // Validate: maxSpend phải lớn hơn minSpend (nếu có)
        if (segment.getMaxSpend() != null && segment.getMaxSpend() <= segment.getMinSpend()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "maxSpend phải lớn hơn minSpend");
        }

        segment = customerSegmentRepository.save(segment);
        return mapToResponse(segment);
    }

    public void deleteSegment(String id) {
        CustomerSegment segment = customerSegmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEGMENT_NOT_FOUND));
        customerSegmentRepository.delete(segment);
    }

    public CustomerSegmentResponse getSegmentById(String id) {
        CustomerSegment segment = customerSegmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SEGMENT_NOT_FOUND));
        return mapToResponse(segment);
    }

    public List<CustomerSegmentResponse> getAllSegments() {
        return customerSegmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public CustomerSegment findSegmentForSpend(Double totalSpend) {
        List<CustomerSegment> segments = customerSegmentRepository.findAllByOrderByLevelDesc();
        
        for (CustomerSegment segment : segments) {
            boolean matchMin = totalSpend >= segment.getMinSpend();
            boolean matchMax = segment.getMaxSpend() == null || totalSpend < segment.getMaxSpend();
            
            if (matchMin && matchMax) {
                return segment;
            }
        }
        
        return null;
    }

    private CustomerSegmentResponse mapToResponse(CustomerSegment segment) {
        return CustomerSegmentResponse.builder()
                .id(segment.getId())
                .name(segment.getName())
                .minSpend(segment.getMinSpend())
                .maxSpend(segment.getMaxSpend())
                .level(segment.getLevel())
                .description(segment.getDescription())
                .createdAt(segment.getCreatedAt())
                .updatedAt(segment.getUpdatedAt())
                .build();
    }
}

