package com.example.cellex.services.analytics;

import com.example.cellex.dtos.response.analytics.*;
import com.example.cellex.dtos.response.analytics.AdminMainDashboardResponse.*;
import com.example.cellex.dtos.response.analytics.ChartDataPoint.*;
import com.example.cellex.dtos.response.analytics.CustomerAnalyticsResponse.*;
import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.Role;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.order.OrderRepository;
import com.example.cellex.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý Customer Analytics
 * Cung cấp các số liệu chi tiết về khách hàng cho Admin
 * 
 * @author Cellex Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Lấy Customer Analytics Dashboard với time filter
     * 
     * @param startDate Ngày bắt đầu
     * @param endDate Ngày kết thúc
     * @return CustomerAnalyticsResponse
     */
    public CustomerAnalyticsResponse getCustomerAnalytics(LocalDate startDate, LocalDate endDate) {
        DateRangeInfo dateRange = calculateDateRange(startDate, endDate);
        
        LocalDateTime startDateTime = dateRange.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = dateRange.getEndDate().atTime(LocalTime.MAX);
        LocalDateTime prevStartDateTime = dateRange.getPreviousStartDate().atStartOfDay();
        LocalDateTime prevEndDateTime = dateRange.getPreviousEndDate().atTime(LocalTime.MAX);

        // Build Summary Cards
        List<DashboardSummaryCard> summaryCards = buildCustomerSummaryCards(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Overview
        CustomerOverview overview = buildCustomerOverview(
                startDateTime, endDateTime, prevStartDateTime, prevEndDateTime);

        // Build Charts
        CustomerCharts charts = buildCustomerCharts(dateRange.getStartDate(), dateRange.getEndDate());

        // Build Segments
        CustomerSegments segments = buildCustomerSegments(startDateTime, endDateTime);

        // Top Customers
        List<TopCustomerItem> topCustomers = getTopCustomersBySpending(startDateTime, endDateTime, 10);

        // Recent Customers
        List<RecentUserItem> recentCustomers = getRecentCustomers(10);

        return CustomerAnalyticsResponse.builder()
                .dateRange(dateRange)
                .summaryCards(summaryCards)
                .overview(overview)
                .charts(charts)
                .segments(segments)
                .topCustomers(topCustomers)
                .recentCustomers(recentCustomers)
                .build();
    }

    /**
     * Build 3 Summary Cards chính cho Customer Analytics
     */
    private List<DashboardSummaryCard> buildCustomerSummaryCards(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        List<DashboardSummaryCard> cards = new ArrayList<>();

        // Card 1: Tổng khách hàng
        long totalCustomers = userRepository.countByRole(Role.USER);
        long prevTotalCustomers = userRepository.countByRoleAndCreatedAtBefore(Role.USER, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Tổng khách hàng",
                (double) totalCustomers,
                (double) prevTotalCustomers,
                "người",
                DashboardSummaryCard.MetricType.NUMBER,
                "users",
                "Tổng số khách hàng đã đăng ký"
        ));

        // Card 2: Khách hàng mới (trong kỳ)
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(Role.USER, startDateTime, endDateTime);
        long prevNewCustomers = userRepository.countByRoleAndCreatedAtBetween(Role.USER, prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Khách hàng mới",
                (double) newCustomers,
                (double) prevNewCustomers,
                "người",
                DashboardSummaryCard.MetricType.NUMBER,
                "user-plus",
                "Số khách hàng mới đăng ký trong kỳ"
        ));

        // Card 3: Khách hàng hoạt động (có đơn trong kỳ)
        long activeCustomers = getActiveCustomersCount(startDateTime, endDateTime);
        long prevActiveCustomers = getActiveCustomersCount(prevStartDateTime, prevEndDateTime);
        cards.add(DashboardSummaryCard.create(
                "Khách hàng hoạt động",
                (double) activeCustomers,
                (double) prevActiveCustomers,
                "người",
                DashboardSummaryCard.MetricType.NUMBER,
                "user-check",
                "Số khách hàng có đơn hàng trong kỳ"
        ));

        return cards;
    }

    /**
     * Build Customer Overview
     */
    private CustomerOverview buildCustomerOverview(
            LocalDateTime startDateTime, LocalDateTime endDateTime,
            LocalDateTime prevStartDateTime, LocalDateTime prevEndDateTime) {
        
        long totalCustomers = userRepository.countByRole(Role.USER);
        
        // New customers
        long newCustomers = userRepository.countByRoleAndCreatedAtBetween(Role.USER, startDateTime, endDateTime);
        long prevNewCustomers = userRepository.countByRoleAndCreatedAtBetween(Role.USER, prevStartDateTime, prevEndDateTime);
        double newCustomersChange = prevNewCustomers > 0 ? 
                ((double)(newCustomers - prevNewCustomers) / prevNewCustomers) * 100 : 0;

        // Active customers
        long activeCustomers = getActiveCustomersCount(startDateTime, endDateTime);
        long prevActiveCustomers = getActiveCustomersCount(prevStartDateTime, prevEndDateTime);
        double activeCustomersChange = prevActiveCustomers > 0 ? 
                ((double)(activeCustomers - prevActiveCustomers) / prevActiveCustomers) * 100 : 0;

        // Return rate (customers with > 1 order)
        double returnRate = calculateReturnRate(startDateTime, endDateTime);
        double prevReturnRate = calculateReturnRate(prevStartDateTime, prevEndDateTime);
        double returnRateChange = prevReturnRate > 0 ? 
                ((returnRate - prevReturnRate) / prevReturnRate) * 100 : 0;

        // Average Customer Value
        double acv = calculateAverageCustomerValue(startDateTime, endDateTime);
        double prevAcv = calculateAverageCustomerValue(prevStartDateTime, prevEndDateTime);
        double acvChange = prevAcv > 0 ? ((acv - prevAcv) / prevAcv) * 100 : 0;

        // Average orders per customer
        long totalOrders = orderRepository.countOrdersByDateRange(startDateTime, endDateTime);
        double avgOrdersPerCustomer = activeCustomers > 0 ? (double) totalOrders / activeCustomers : 0;

        return CustomerOverview.builder()
                .totalCustomers(totalCustomers)
                .newCustomers(newCustomers)
                .newCustomersChange(Math.round(newCustomersChange * 100.0) / 100.0)
                .activeCustomers(activeCustomers)
                .activeCustomersChange(Math.round(activeCustomersChange * 100.0) / 100.0)
                .returnRate(Math.round(returnRate * 100.0) / 100.0)
                .returnRateChange(Math.round(returnRateChange * 100.0) / 100.0)
                .averageCustomerValue(Math.round(acv * 100.0) / 100.0)
                .acvChange(Math.round(acvChange * 100.0) / 100.0)
                .averageOrdersPerCustomer(Math.round(avgOrdersPerCustomer * 100.0) / 100.0)
                .build();
    }

    /**
     * Build Customer Charts
     */
    private CustomerCharts buildCustomerCharts(LocalDate startDate, LocalDate endDate) {
        return CustomerCharts.builder()
                .newCustomersChart(buildNewCustomersTimeSeriesChart(startDate, endDate))
                .activeCustomersChart(buildActiveCustomersTimeSeriesChart(startDate, endDate))
                .customerSegmentChart(buildCustomerSegmentPieChart())
                .customersByOrderCountChart(buildCustomersByOrderCountPieChart())
                .customerSpendingChart(buildCustomerSpendingTimeSeriesChart(startDate, endDate))
                .build();
    }

    /**
     * Build New Customers Time Series Chart
     */
    private TimeSeriesChart buildNewCustomersTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        long total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long count = userRepository.countByRoleAndCreatedAtBetween(
                    Role.USER, date.atStartOfDay(), date.atTime(LocalTime.MAX));
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value((double) count)
                    .date(date)
                    .build());
            total += count;
        }

        return TimeSeriesChart.builder()
                .title("Khách hàng mới theo ngày")
                .chartType(ChartType.LINE)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Số khách hàng")
                .unit("người")
                .total((double) total)
                .average(dataPoints.isEmpty() ? 0 : (double) total / dataPoints.size())
                .build();
    }

    /**
     * Build Active Customers Time Series Chart
     */
    private TimeSeriesChart buildActiveCustomersTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        long total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            long count = getActiveCustomersCount(date.atStartOfDay(), date.atTime(LocalTime.MAX));
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value((double) count)
                    .date(date)
                    .build());
            total += count;
        }

        return TimeSeriesChart.builder()
                .title("Khách hàng hoạt động theo ngày")
                .chartType(ChartType.AREA)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Số khách hàng")
                .unit("người")
                .total((double) total)
                .average(dataPoints.isEmpty() ? 0 : (double) total / dataPoints.size())
                .build();
    }

    /**
     * Build Customer Segment Pie Chart
     */
    private PieChartData buildCustomerSegmentPieChart() {
        List<PieSlice> slices = new ArrayList<>();
        long totalCustomers = userRepository.countByRole(Role.USER);

        // Calculate segments based on spending
        List<Order> allCompletedOrders = orderRepository.findAllCompletedPaidOrders();
        Map<String, Double> spendingByUser = new HashMap<>();
        
        for (Order order : allCompletedOrders) {
            spendingByUser.merge(order.getUserId(), order.getTotalAmount(), Double::sum);
        }

        // Segment by spending thresholds
        long vipCount = spendingByUser.values().stream().filter(s -> s >= 10_000_000).count();
        long regularCount = spendingByUser.values().stream().filter(s -> s >= 1_000_000 && s < 10_000_000).count();
        long newCount = spendingByUser.values().stream().filter(s -> s > 0 && s < 1_000_000).count();
        long inactiveCount = totalCustomers - spendingByUser.size();

        slices.add(PieSlice.builder()
                .label("VIP (≥10tr)")
                .value((double) vipCount)
                .percentage(totalCustomers > 0 ? Math.round((double) vipCount / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#FFB020")
                .build());
        slices.add(PieSlice.builder()
                .label("Thường xuyên (1-10tr)")
                .value((double) regularCount)
                .percentage(totalCustomers > 0 ? Math.round((double) regularCount / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#36B37E")
                .build());
        slices.add(PieSlice.builder()
                .label("Mới (<1tr)")
                .value((double) newCount)
                .percentage(totalCustomers > 0 ? Math.round((double) newCount / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#3366FF")
                .build());
        slices.add(PieSlice.builder()
                .label("Chưa mua hàng")
                .value((double) inactiveCount)
                .percentage(totalCustomers > 0 ? Math.round((double) inactiveCount / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#8B8D97")
                .build());

        return PieChartData.builder()
                .title("Phân khúc khách hàng theo chi tiêu")
                .slices(slices)
                .total((double) totalCustomers)
                .build();
    }

    /**
     * Build Customers by Order Count Pie Chart
     */
    private PieChartData buildCustomersByOrderCountPieChart() {
        List<PieSlice> slices = new ArrayList<>();
        long totalCustomers = userRepository.countByRole(Role.USER);

        // Count orders per user
        List<Order> allOrders = orderRepository.findAll();
        Map<String, Long> orderCountByUser = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));

        long oneOrder = orderCountByUser.values().stream().filter(c -> c == 1).count();
        long twoToFive = orderCountByUser.values().stream().filter(c -> c >= 2 && c <= 5).count();
        long sixToTen = orderCountByUser.values().stream().filter(c -> c >= 6 && c <= 10).count();
        long moreThanTen = orderCountByUser.values().stream().filter(c -> c > 10).count();
        long noOrders = totalCustomers - orderCountByUser.size();

        slices.add(PieSlice.builder()
                .label("1 đơn").value((double) oneOrder)
                .percentage(totalCustomers > 0 ? Math.round((double) oneOrder / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#3366FF").build());
        slices.add(PieSlice.builder()
                .label("2-5 đơn").value((double) twoToFive)
                .percentage(totalCustomers > 0 ? Math.round((double) twoToFive / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#00B8D9").build());
        slices.add(PieSlice.builder()
                .label("6-10 đơn").value((double) sixToTen)
                .percentage(totalCustomers > 0 ? Math.round((double) sixToTen / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#36B37E").build());
        slices.add(PieSlice.builder()
                .label(">10 đơn").value((double) moreThanTen)
                .percentage(totalCustomers > 0 ? Math.round((double) moreThanTen / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#FFB020").build());
        slices.add(PieSlice.builder()
                .label("Chưa mua").value((double) noOrders)
                .percentage(totalCustomers > 0 ? Math.round((double) noOrders / totalCustomers * 10000.0) / 100.0 : 0)
                .color("#8B8D97").build());

        return PieChartData.builder()
                .title("Phân bổ khách hàng theo số đơn hàng")
                .slices(slices)
                .total((double) totalCustomers)
                .build();
    }

    /**
     * Build Customer Spending Time Series Chart
     */
    private TimeSeriesChart buildCustomerSpendingTimeSeriesChart(LocalDate startDate, LocalDate endDate) {
        List<ChartDataPoint> dataPoints = new ArrayList<>();
        double total = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(
                    date.atStartOfDay(), date.atTime(LocalTime.MAX));
            double spending = orders.stream().mapToDouble(Order::getTotalAmount).sum();
            dataPoints.add(ChartDataPoint.builder()
                    .label(date.format(formatter))
                    .value(spending)
                    .date(date)
                    .build());
            total += spending;
        }

        return TimeSeriesChart.builder()
                .title("Tổng chi tiêu của khách hàng theo ngày")
                .chartType(ChartType.AREA)
                .data(dataPoints)
                .xAxisLabel("Ngày")
                .yAxisLabel("Chi tiêu (VND)")
                .unit("VND")
                .total(total)
                .average(dataPoints.isEmpty() ? 0 : total / dataPoints.size())
                .build();
    }

    /**
     * Build Customer Segments
     */
    private CustomerSegments buildCustomerSegments(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Order> allCompletedOrders = orderRepository.findAllCompletedPaidOrders();
        Map<String, Double> spendingByUser = new HashMap<>();
        Map<String, Long> orderCountByUser = new HashMap<>();

        for (Order order : allCompletedOrders) {
            spendingByUser.merge(order.getUserId(), order.getTotalAmount(), Double::sum);
            orderCountByUser.merge(order.getUserId(), 1L, Long::sum);
        }

        long totalCustomers = userRepository.countByRole(Role.USER);

        // VIP Segment (≥10M)
        List<String> vipUserIds = spendingByUser.entrySet().stream()
                .filter(e -> e.getValue() >= 10_000_000)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        // Regular Segment (1-10M)
        List<String> regularUserIds = spendingByUser.entrySet().stream()
                .filter(e -> e.getValue() >= 1_000_000 && e.getValue() < 10_000_000)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // New customers (created in period and <1M spending)
        List<User> newUsersInPeriod = userRepository.findByRoleAndCreatedAtBetween(
                Role.USER, startDateTime, endDateTime);
        List<String> newUserIds = newUsersInPeriod.stream()
                .filter(u -> spendingByUser.getOrDefault(u.getId(), 0.0) < 1_000_000)
                .map(User::getId)
                .collect(Collectors.toList());

        // Inactive (no orders in 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Set<String> recentBuyers = orderRepository.findOrdersByDateRange(thirtyDaysAgo, LocalDateTime.now())
                .stream()
                .map(Order::getUserId)
                .collect(Collectors.toSet());
        long inactiveCount = totalCustomers - recentBuyers.size();

        return CustomerSegments.builder()
                .vipCustomers(buildSegmentInfo("VIP", vipUserIds, spendingByUser, orderCountByUser, totalCustomers))
                .regularCustomers(buildSegmentInfo("Thường xuyên", regularUserIds, spendingByUser, orderCountByUser, totalCustomers))
                .newCustomers(buildSegmentInfo("Mới", newUserIds, spendingByUser, orderCountByUser, totalCustomers))
                .inactiveCustomers(SegmentInfo.builder()
                        .segmentName("Không hoạt động")
                        .count(inactiveCount)
                        .percentage(totalCustomers > 0 ? Math.round((double) inactiveCount / totalCustomers * 10000.0) / 100.0 : 0)
                        .averageSpending(0.0)
                        .averageOrders(0.0)
                        .build())
                .build();
    }

    private SegmentInfo buildSegmentInfo(String name, List<String> userIds, 
                                          Map<String, Double> spendingByUser,
                                          Map<String, Long> orderCountByUser,
                                          long totalCustomers) {
        long count = userIds.size();
        double avgSpending = userIds.stream()
                .mapToDouble(id -> spendingByUser.getOrDefault(id, 0.0))
                .average()
                .orElse(0);
        double avgOrders = userIds.stream()
                .mapToDouble(id -> orderCountByUser.getOrDefault(id, 0L))
                .average()
                .orElse(0);

        return SegmentInfo.builder()
                .segmentName(name)
                .count(count)
                .percentage(totalCustomers > 0 ? Math.round((double) count / totalCustomers * 10000.0) / 100.0 : 0)
                .averageSpending(Math.round(avgSpending * 100.0) / 100.0)
                .averageOrders(Math.round(avgOrders * 100.0) / 100.0)
                .build();
    }

    /**
     * Get Top Customers by Spending
     */
    private List<TopCustomerItem> getTopCustomersBySpending(
            LocalDateTime start, LocalDateTime end, int limit) {
        
        List<Order> completedOrders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        Map<String, Double> spendingByUser = new HashMap<>();
        Map<String, Long> orderCountByUser = new HashMap<>();
        Map<String, LocalDateTime> lastOrderByUser = new HashMap<>();

        for (Order order : completedOrders) {
            String userId = order.getUserId();
            spendingByUser.merge(userId, order.getTotalAmount(), Double::sum);
            orderCountByUser.merge(userId, 1L, Long::sum);
            lastOrderByUser.compute(userId, (k, v) -> 
                    v == null || (order.getCreatedAt() != null && order.getCreatedAt().isAfter(v)) 
                    ? order.getCreatedAt() : v);
        }

        return spendingByUser.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(entry -> {
                    String userId = entry.getKey();
                    User user = userRepository.findById(userId).orElse(null);
                    return TopCustomerItem.builder()
                            .rank(0) // Will be set later
                            .userId(userId)
                            .fullName(user != null ? user.getFullName() : "Unknown")
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .totalSpent(entry.getValue())
                            .orderCount(orderCountByUser.getOrDefault(userId, 0L))
                            .lastOrderDate(lastOrderByUser.get(userId))
                            .build();
                })
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).setRank(i + 1);
                    }
                    return list;
                }));
    }

    /**
     * Get Recent Customers
     */
    private List<RecentUserItem> getRecentCustomers(int limit) {
        return userRepository.findByRole(Role.USER, PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(user -> RecentUserItem.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .avatarUrl(user.getAvatarUrl())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    private DateRangeInfo calculateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        long periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(periodDays - 1);

        return DateRangeInfo.builder()
                .startDate(startDate)
                .endDate(endDate)
                .period("CUSTOM")
                .previousStartDate(prevStartDate)
                .previousEndDate(prevEndDate)
                .build();
    }

    private long getActiveCustomersCount(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findOrdersByDateRange(start, end);
        return orders.stream().map(Order::getUserId).distinct().count();
    }

    private double calculateReturnRate(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findOrdersByDateRange(start, end);
        Map<String, Long> orderCountByUser = orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, Collectors.counting()));
        
        long totalBuyers = orderCountByUser.size();
        long returningBuyers = orderCountByUser.values().stream().filter(c -> c > 1).count();
        
        return totalBuyers > 0 ? (double) returningBuyers / totalBuyers * 100 : 0;
    }

    private double calculateAverageCustomerValue(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findCompletedPaidOrdersByDateRange(start, end);
        Map<String, Double> spendingByUser = orders.stream()
                .collect(Collectors.groupingBy(Order::getUserId, 
                        Collectors.summingDouble(Order::getTotalAmount)));
        
        return spendingByUser.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }
}
