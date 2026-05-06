package com.example.cellex.seeder;

import com.example.cellex.models.product.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductPopularityTracker {

    private final Set<String> hotProductIds = ConcurrentHashMap.newKeySet();
    private volatile List<Product> hotProducts = List.of();
    private volatile List<Product> normalProducts = List.of();
    private volatile Map<String, List<Product>> productsByCategory = Map.of();

    public void initialize(List<Product> products) {
        if (products == null || products.isEmpty()) {
            hotProductIds.clear();
            hotProducts = List.of();
            normalProducts = List.of();
            productsByCategory = Map.of();
            return;
        }

        List<Product> sorted = products.stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getId() != null)
                .sorted(Comparator
                        .comparingInt((Product product) -> safeInt(product.getPurchaseCount()))
                        .reversed()
                        .thenComparing(Product::getId))
                .toList();

        int hotCount = Math.max(1, (int) Math.ceil(sorted.size() * 0.2));
        List<Product> hot = new ArrayList<>(sorted.subList(0, Math.min(hotCount, sorted.size())));
        List<Product> normal = new ArrayList<>(sorted.subList(Math.min(hotCount, sorted.size()), sorted.size()));

        hotProductIds.clear();
        hot.forEach(product -> hotProductIds.add(product.getId()));

        this.hotProducts = Collections.unmodifiableList(hot);
        this.normalProducts = Collections.unmodifiableList(normal);
        this.productsByCategory = sorted.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.toUnmodifiableList()));

        log.info("Product popularity initialized: total={}, hot={}, normal={}", sorted.size(), hot.size(), normal.size());
    }

    public Product selectProduct(List<Product> candidates, String preferredCategoryId) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        List<Product> source = candidates.stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getId() != null)
                .toList();

        if (source.isEmpty()) {
            return null;
        }

        List<Product> scopedCandidates = source;
        boolean pickPreferredCategory = preferredCategoryId != null && ThreadLocalRandom.current().nextDouble() < 0.7;

        if (pickPreferredCategory) {
            List<Product> preferredPool = source.stream()
                    .filter(product -> preferredCategoryId.equals(product.getCategoryId()))
                    .toList();

            if (!preferredPool.isEmpty()) {
                scopedCandidates = preferredPool;
            }
        }

        List<Product> hotInScope = scopedCandidates.stream()
                .filter(product -> hotProductIds.contains(product.getId()))
                .toList();

        List<Product> normalInScope = scopedCandidates.stream()
                .filter(product -> !hotProductIds.contains(product.getId()))
                .toList();

        boolean chooseHot = ThreadLocalRandom.current().nextDouble() < 0.8;

        if (chooseHot) {
            if (!hotInScope.isEmpty()) {
                return randomFrom(hotInScope);
            }
            if (!normalInScope.isEmpty()) {
                return randomFrom(normalInScope);
            }
        } else {
            if (!normalInScope.isEmpty()) {
                return randomFrom(normalInScope);
            }
            if (!hotInScope.isEmpty()) {
                return randomFrom(hotInScope);
            }
        }

        return randomFrom(scopedCandidates);
    }

    public List<Product> getHotProducts() {
        return hotProducts;
    }

    public boolean isHotProduct(String productId) {
        return productId != null && hotProductIds.contains(productId);
    }

    private Product randomFrom(List<Product> products) {
        return products.get(ThreadLocalRandom.current().nextInt(products.size()));
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
