package com.example.cellex.seeder;

import com.example.cellex.enums.OrderStatus;
import com.example.cellex.enums.Role;
import com.example.cellex.models.order.Order;
import com.example.cellex.models.order.OrderItem;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.recommendation.UserInteraction;
import com.example.cellex.models.review.Review;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.product.ProductRepository;
import com.example.cellex.repositories.recommendation.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionMatrixSeeder {

    private static final int BATCH_SIZE = 100;

    private final UserInteractionRepository userInteractionRepository;
    private final UserBehaviorSimulator userBehaviorSimulator;
    private final ProductPopularityTracker productPopularityTracker;
    private final ProductRepository productRepository;

    public List<UserInteraction> seedInteractions(List<User> users, List<Product> products, List<Order> allOrders) {
        return seedInteractions(users, products, allOrders, List.of());
    }

    public List<UserInteraction> seedInteractions(
            List<User> users,
            List<Product> products,
            List<Order> allOrders,
            List<Review> reviews
    ) {
        if (users == null || users.isEmpty() || products == null || products.isEmpty()) {
            return List.of();
        }

        List<User> normalUsers = users.stream()
                .filter(Objects::nonNull)
                .filter(user -> user.getId() != null)
                .filter(user -> user.getRole() == Role.USER)
                .toList();

        if (normalUsers.isEmpty()) {
            return List.of();
        }

        Map<String, Product> productById = products.stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product, (left, right) -> left, HashMap::new));

        if (productById.isEmpty()) {
            return List.of();
        }

        Map<String, MutableInteraction> matrix = new HashMap<>();

        seedViews(normalUsers, products, matrix);
        seedCartAdds(matrix);
        fillPurchasesFromOrders(allOrders, productById, matrix);
        fillReviewsFromCreatedReviews(reviews, productById, matrix);

        List<UserInteraction> interactions = buildUserInteractions(matrix.values());
        saveInteractionsInBatches(interactions);
        updateProductPurchaseCount(products, interactions);

        log.info("Seeded interaction matrix size={}", interactions.size());
        return interactions;
    }

    private void seedViews(List<User> users, List<Product> products, Map<String, MutableInteraction> matrix) {
        for (User user : users) {
            int viewEvents = ThreadLocalRandom.current().nextInt(20, 61);
            List<String> preferredCategories = userBehaviorSimulator.getPreferredCategories(user.getId());

            for (int i = 0; i < viewEvents; i++) {
                String targetCategory = pickTargetCategory(preferredCategories);
                Product product = selectProductForView(products, targetCategory);

                if (product == null || product.getId() == null) {
                    continue;
                }

                MutableInteraction interaction = getOrCreate(matrix, user.getId(), product);
                int increment = userBehaviorSimulator.getViewCount(user.getId(), product.getCategoryId());
                interaction.viewCount = Math.min(8, interaction.viewCount + Math.max(1, increment));
            }
        }
    }

    private void seedCartAdds(Map<String, MutableInteraction> matrix) {
        for (MutableInteraction interaction : matrix.values()) {
            if (interaction.viewCount <= 0) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                interaction.cartCount = ThreadLocalRandom.current().nextInt(1, 4);
            }
        }
    }

    private void fillPurchasesFromOrders(
            List<Order> allOrders,
            Map<String, Product> productById,
            Map<String, MutableInteraction> matrix
    ) {
        if (allOrders == null || allOrders.isEmpty()) {
            return;
        }

        for (Order order : allOrders) {
            if (order == null || order.getStatus() != OrderStatus.DELIVERED || order.getUserId() == null || order.getItems() == null) {
                continue;
            }

            for (OrderItem item : order.getItems()) {
                if (item == null || item.getProductId() == null) {
                    continue;
                }

                Product product = productById.get(item.getProductId());
                if (product == null) {
                    continue;
                }

                MutableInteraction interaction = getOrCreate(matrix, order.getUserId(), product);
                int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                interaction.purchaseCount += Math.max(0, quantity);
            }

        }
    }

    private void fillReviewsFromCreatedReviews(
            List<Review> reviews,
            Map<String, Product> productById,
            Map<String, MutableInteraction> matrix
    ) {
        if (reviews == null || reviews.isEmpty()) {
            return;
        }

        for (Review review : reviews) {
            if (review == null || review.getUserId() == null || review.getProductId() == null) {
                continue;
            }

            Product product = productById.get(review.getProductId());
            if (product == null) {
                continue;
            }

            MutableInteraction interaction = getOrCreate(matrix, review.getUserId(), product);
            interaction.reviewCount = 1;
        }
    }

    private List<UserInteraction> buildUserInteractions(java.util.Collection<MutableInteraction> matrixValues) {
        List<UserInteraction> interactions = new ArrayList<>();

        for (MutableInteraction item : matrixValues) {
            UserInteraction interaction = UserInteraction.builder()
                    .userId(item.userId)
                    .productId(item.productId)
                    .categoryId(item.categoryId)
                    .viewCount(item.viewCount)
                    .cartCount(item.cartCount)
                    .purchaseCount(item.purchaseCount)
                    .reviewCount(item.reviewCount)
                    .createdAt(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 120)))
                    .updatedAt(LocalDateTime.now())
                    .build();
            interaction.calculateTotalScore();
            interactions.add(interaction);
        }

        return interactions;
    }

    private void saveInteractionsInBatches(List<UserInteraction> interactions) {
        if (interactions == null || interactions.isEmpty()) {
            return;
        }

        for (int i = 0; i < interactions.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, interactions.size());
            userInteractionRepository.saveAll(interactions.subList(i, end));
        }
    }

    private void updateProductPurchaseCount(List<Product> products, List<UserInteraction> interactions) {
        if (products == null || products.isEmpty() || interactions == null || interactions.isEmpty()) {
            return;
        }

        Map<String, Integer> purchasesByProduct = new HashMap<>();
        for (UserInteraction interaction : interactions) {
            if (interaction == null || interaction.getProductId() == null) {
                continue;
            }

            int purchaseCount = interaction.getPurchaseCount() == null ? 0 : interaction.getPurchaseCount();
            purchasesByProduct.merge(interaction.getProductId(), purchaseCount, Integer::sum);
        }

        List<Product> updates = new ArrayList<>();
        for (Product product : products) {
            if (product == null || product.getId() == null) {
                continue;
            }

            int totalPurchases = purchasesByProduct.getOrDefault(product.getId(), 0);
            product.setPurchaseCount(totalPurchases);
            product.setUpdatedAt(LocalDateTime.now());
            updates.add(product);
        }

        for (int i = 0; i < updates.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, updates.size());
            productRepository.saveAll(updates.subList(i, end));
        }
    }

    private Product selectProductForView(List<Product> products, String preferredCategoryId) {
        List<Product> candidates = products;

        if (preferredCategoryId != null && ThreadLocalRandom.current().nextDouble() < 0.7) {
            List<Product> preferredProducts = products.stream()
                    .filter(product -> Objects.equals(preferredCategoryId, product.getCategoryId()))
                    .toList();
            if (!preferredProducts.isEmpty()) {
                candidates = preferredProducts;
            }
        }

        List<Product> hotProducts = candidates.stream()
                .filter(product -> productPopularityTracker.isHotProduct(product.getId()))
                .toList();

        List<Product> normalProducts = candidates.stream()
                .filter(product -> !productPopularityTracker.isHotProduct(product.getId()))
                .toList();

        if (hotProducts.isEmpty() && normalProducts.isEmpty()) {
            return null;
        }

        if (hotProducts.isEmpty()) {
            return pickRandom(normalProducts);
        }

        if (normalProducts.isEmpty()) {
            return pickRandom(hotProducts);
        }

        double hotWeight = hotProducts.size() * 3.0;
        double normalWeight = normalProducts.size();
        double hotProbability = hotWeight / (hotWeight + normalWeight);

        if (ThreadLocalRandom.current().nextDouble() < hotProbability) {
            return pickRandom(hotProducts);
        }

        return pickRandom(normalProducts);
    }

    private Product pickRandom(List<Product> products) {
        return products.get(ThreadLocalRandom.current().nextInt(products.size()));
    }

    private String pickTargetCategory(List<String> preferredCategories) {
        if (preferredCategories == null || preferredCategories.isEmpty()) {
            return null;
        }

        if (preferredCategories.size() == 1 || ThreadLocalRandom.current().nextDouble() < 0.7) {
            return preferredCategories.get(0);
        }

        int index = ThreadLocalRandom.current().nextInt(1, preferredCategories.size());
        return preferredCategories.get(index);
    }

    private MutableInteraction getOrCreate(Map<String, MutableInteraction> matrix, String userId, Product product) {
        String key = userId + "::" + product.getId();

        return matrix.computeIfAbsent(key, ignored -> {
            MutableInteraction interaction = new MutableInteraction();
            interaction.userId = userId;
            interaction.productId = product.getId();
            interaction.categoryId = product.getCategoryId();
            return interaction;
        });
    }

    private static class MutableInteraction {
        private String userId;
        private String productId;
        private String categoryId;
        private int viewCount;
        private int cartCount;
        private int purchaseCount;
        private int reviewCount;
    }
}
