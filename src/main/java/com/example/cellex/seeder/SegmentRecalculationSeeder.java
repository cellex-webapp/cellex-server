package com.example.cellex.seeder;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.Role;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.segment.CustomerSegment;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.user.UserRepository;
import com.example.cellex.services.segment.CustomerSegmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class SegmentRecalculationSeeder {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CustomerSegmentService customerSegmentService;

    @Transactional
    public void recalculate(List<User> users) {
        List<User> sourceUsers = users == null || users.isEmpty() ? userRepository.findAll() : users;

        List<User> updates = new ArrayList<>();
        for (User user : sourceUsers) {
            if (user == null || user.getId() == null || user.getRole() != Role.USER) {
                continue;
            }

            List<Order> userOrders = orderRepository.findByUserId(
                    user.getId(),
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );

            double totalSpend = userOrders.stream()
                    .filter(Objects::nonNull)
                    .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                    .filter(order -> Boolean.TRUE.equals(order.getIsPaid()))
                    .map(Order::getTotalAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            user.setTotalSpend(totalSpend);

            CustomerSegment segment = customerSegmentService.findSegmentForSpend(totalSpend);
            user.setCustomerSegmentId(segment != null ? segment.getId() : null);

            updates.add(user);
            log.info(
                    "Segment assigned: userId={}, segment={}, totalSpend={}",
                    user.getId(),
                    segment != null ? segment.getName() : null,
                    totalSpend
            );
        }

        if (!updates.isEmpty()) {
            userRepository.saveAll(updates);
        }
    }
}
