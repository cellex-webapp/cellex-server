package com.example.cellex.seeder;

import com.example.cellex.models.category.Category;
import com.example.cellex.models.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorSimulator {

    private final Map<String, UserArchetype> archetypeByUserId = new ConcurrentHashMap<>();
    private final Map<String, List<String>> preferredCategoriesByUserId = new ConcurrentHashMap<>();
    private final Map<String, Integer> orderCountByUserId = new ConcurrentHashMap<>();
    private final Map<String, Long> seedByUserId = new ConcurrentHashMap<>();

    public void assignArchetypes(List<User> users, List<Category> categories) {
        archetypeByUserId.clear();
        preferredCategoriesByUserId.clear();
        orderCountByUserId.clear();
        seedByUserId.clear();

        if (users == null || users.isEmpty()) {
            return;
        }

        List<String> categoryIds = categories == null
                ? List.of()
                : categories.stream()
                .map(Category::getId)
                .filter(Objects::nonNull)
                .toList();

        AtomicInteger power = new AtomicInteger();
        AtomicInteger regular = new AtomicInteger();
        AtomicInteger casual = new AtomicInteger();
        AtomicInteger window = new AtomicInteger();

        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }

            long seed = buildUserSeed(user);
            UserArchetype archetype = resolveArchetype(seed);
            List<String> preferredCategories = resolvePreferredCategories(seed, categoryIds);
            int orderCount = getOrderCountForSeed(archetype, "order_count:" + seed);

            archetypeByUserId.put(user.getId(), archetype);
            preferredCategoriesByUserId.put(user.getId(), preferredCategories);
            orderCountByUserId.put(user.getId(), orderCount);
            seedByUserId.put(user.getId(), seed);

            switch (archetype) {
                case POWER_BUYER -> power.incrementAndGet();
                case REGULAR_BUYER -> regular.incrementAndGet();
                case CASUAL_BUYER -> casual.incrementAndGet();
                case WINDOW_SHOPPER -> window.incrementAndGet();
                default -> {
                }
            }
        }

        log.info(
                "User archetypes assigned: total={}, POWER={}, REGULAR={}, CASUAL={}, WINDOW={}",
                archetypeByUserId.size(), power.get(), regular.get(), casual.get(), window.get()
        );
    }

    public UserArchetype getArchetype(String userId) {
        if (userId == null) {
            return UserArchetype.CASUAL_BUYER;
        }
        return archetypeByUserId.getOrDefault(userId, UserArchetype.CASUAL_BUYER);
    }

    public UserArchetype getArchetype(User user) {
        return user == null ? UserArchetype.CASUAL_BUYER : getArchetype(user.getId());
    }

    public List<String> getPreferredCategories(String userId) {
        if (userId == null) {
            return List.of();
        }
        return preferredCategoriesByUserId.getOrDefault(userId, List.of());
    }

    public List<String> getPreferredCategories(User user) {
        return user == null ? List.of() : getPreferredCategories(user.getId());
    }

    public int getOrderCount(UserArchetype archetype) {
        return getOrderCountForSeed(archetype, String.valueOf(archetype));
    }

    public int getOrderCount(String userId) {
        if (userId == null) {
            return getOrderCountForSeed(UserArchetype.CASUAL_BUYER, "order_count:null");
        }
        Integer fixedOrderCount = orderCountByUserId.get(userId);
        if (fixedOrderCount != null) {
            return fixedOrderCount;
        }

        UserArchetype archetype = getArchetype(userId);
        long seed = getUserSeed(userId);
        return getOrderCountForSeed(archetype, "order_count_fallback:" + seed);
    }

    public boolean willReview(String userId) {
        UserArchetype archetype = getArchetype(userId);
        int roll = deterministicInt("review:" + getUserSeed(userId), 0, 99);

        return switch (archetype) {
            case POWER_BUYER -> roll < 70;
            case REGULAR_BUYER -> roll < 40;
            case CASUAL_BUYER -> roll < 20;
            case WINDOW_SHOPPER -> false;
        };
    }

    public int getViewCount(String userId, String categoryId) {
        List<String> preferred = getPreferredCategories(userId);
        UserArchetype archetype = getArchetype(userId);
        long seed = getUserSeed(userId);

        boolean isPrimary = !preferred.isEmpty() && Objects.equals(preferred.get(0), categoryId);
        boolean isSecondary = !isPrimary && preferred.contains(categoryId);

        return switch (archetype) {
            case POWER_BUYER -> {
                if (isPrimary) {
                    yield deterministicInt(seed + ":view:power:primary:" + categoryId, 5, 8);
                }
                if (isSecondary) {
                    yield deterministicInt(seed + ":view:power:secondary:" + categoryId, 3, 6);
                }
                yield deterministicInt(seed + ":view:power:other:" + categoryId, 1, 3);
            }
            case REGULAR_BUYER -> {
                if (isPrimary) {
                    yield deterministicInt(seed + ":view:regular:primary:" + categoryId, 4, 7);
                }
                if (isSecondary) {
                    yield deterministicInt(seed + ":view:regular:secondary:" + categoryId, 2, 5);
                }
                yield deterministicInt(seed + ":view:regular:other:" + categoryId, 1, 3);
            }
            case CASUAL_BUYER -> {
                if (isPrimary) {
                    yield deterministicInt(seed + ":view:casual:primary:" + categoryId, 2, 5);
                }
                if (isSecondary) {
                    yield deterministicInt(seed + ":view:casual:secondary:" + categoryId, 1, 3);
                }
                yield deterministicInt(seed + ":view:casual:other:" + categoryId, 1, 2);
            }
            case WINDOW_SHOPPER -> {
                if (isPrimary) {
                    yield deterministicInt(seed + ":view:window:primary:" + categoryId, 5, 8);
                }
                if (isSecondary) {
                    yield deterministicInt(seed + ":view:window:secondary:" + categoryId, 3, 6);
                }
                yield deterministicInt(seed + ":view:window:other:" + categoryId, 2, 4);
            }
        };
    }

    private int getOrderCountForSeed(UserArchetype archetype, String seedKey) {
        return switch (archetype) {
            case POWER_BUYER -> deterministicInt(seedKey + ":power", 15, 30);
            case REGULAR_BUYER -> deterministicInt(seedKey + ":regular", 5, 15);
            case CASUAL_BUYER -> deterministicInt(seedKey + ":casual", 1, 5);
            case WINDOW_SHOPPER -> deterministicInt(seedKey + ":window", 0, 1);
        };
    }

    private UserArchetype resolveArchetype(long seed) {
        int bucket = Math.floorMod((int) seed, 100);
        if (bucket < 10) {
            return UserArchetype.POWER_BUYER;
        }
        if (bucket < 35) {
            return UserArchetype.REGULAR_BUYER;
        }
        if (bucket < 75) {
            return UserArchetype.CASUAL_BUYER;
        }
        return UserArchetype.WINDOW_SHOPPER;
    }

    private List<String> resolvePreferredCategories(long seed, List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return List.of();
        }

        List<String> pool = new ArrayList<>(categoryIds);
        int primaryIndex = Math.floorMod((int) seed, pool.size());
        String primary = pool.get(primaryIndex);

        List<String> result = new ArrayList<>();
        result.add(primary);

        pool.remove(primary);
        int secondaryCount = deterministicInt("secondary_count:" + seed, 0, Math.min(2, pool.size()));
        for (int i = 0; i < secondaryCount; i++) {
            int pick = deterministicInt("secondary:" + seed + ":" + i, 0, pool.size() - 1);
            result.add(pool.remove(pick));
        }

        return Collections.unmodifiableList(result);
    }

    private long buildUserSeed(User user) {
        String email = user.getEmail() == null ? "" : user.getEmail().toLowerCase();
        return Integer.toUnsignedLong(email.hashCode());
    }

    private long getUserSeed(String userId) {
        if (userId == null) {
            return 0L;
        }
        return seedByUserId.getOrDefault(userId, Integer.toUnsignedLong(userId.hashCode()));
    }

    private int deterministicInt(String key, int minInclusive, int maxInclusive) {
        if (minInclusive >= maxInclusive) {
            return minInclusive;
        }

        int bound = (maxInclusive - minInclusive) + 1;
        int base = Math.floorMod(Objects.hashCode(key), bound);
        return minInclusive + base;
    }
}
