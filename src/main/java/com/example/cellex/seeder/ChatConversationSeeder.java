package com.example.cellex.seeder;

import com.example.cellex.enums.Role;
import com.example.cellex.models.chat.AIConversation;
import com.example.cellex.models.chat.AIMessage;
import com.example.cellex.models.product.Product;
import com.example.cellex.models.user.User;
import com.example.cellex.repositories.chat.AIConversationRepository;
import com.example.cellex.repositories.chat.AIMessageRepository;
import com.example.cellex.repositories.shop.ShopRepository;
import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatConversationSeeder {

    private static final int BATCH_SIZE = 100;

    private static final List<String> BUYER_TEMPLATES = List.of(
            "So sanh [product A] va [product B], cai nao tot hon?",
            "Tim laptop RAM 16GB duoi [price] trieu",
            "Dien thoai pin trau nhat hien tai la gi?",
            "Tu van tai nghe chong on cho van phong",
            "Phu kien nao phu hop voi [product name]?"
    );

    private static final List<String> VENDOR_TEMPLATES = List.of(
            "Doanh thu thang nay cua shop toi la bao nhieu?",
            "San pham nao ban chay nhat trong 30 ngay qua?",
            "Ton kho cua toi con bao nhieu san pham sap het?",
            "Goi y chien dich khuyen mai cho cuoi thang",
            "Don hang dang cho xac nhan cua toi la bao nhieu?"
    );

    private static final List<String> ADMIN_TEMPLATES = List.of(
            "Tong quan he thong hom nay nhu the nao?",
            "Phan tich phan khuc Gold dang hoat dong ra sao?",
            "Top 5 shop co doanh thu cao nhat thang nay",
            "Goi y dieu chinh nguong phan khuc khach hang"
    );

    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;
    private final ShopRepository shopRepository;
    private final UserBehaviorSimulator userBehaviorSimulator;

    private final Faker faker = new Faker();

    public void seedConversations(List<User> users, List<Product> products) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<ConversationSeedPlan> plans = new ArrayList<>();

        for (User user : users) {
            if (!isEligibleUser(user)) {
                continue;
            }

            int conversationCount = ThreadLocalRandom.current().nextInt(3, 9);
            for (int i = 0; i < conversationCount; i++) {
                plans.add(buildConversationPlan(user, products));
            }
        }

        if (plans.isEmpty()) {
            return;
        }

        int conversationCounter = 0;
        int messageCounter = 0;

        for (int i = 0; i < plans.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, plans.size());
            List<ConversationSeedPlan> batchPlans = plans.subList(i, end);

            List<AIConversation> conversationBatch = batchPlans.stream()
                    .map(ConversationSeedPlan::conversation)
                    .toList();

            List<AIConversation> savedConversations = aiConversationRepository.saveAll(conversationBatch);
            conversationCounter += savedConversations.size();

            List<AIMessage> messageBatch = new ArrayList<>();
            for (int index = 0; index < savedConversations.size(); index++) {
                AIConversation savedConversation = savedConversations.get(index);
                ConversationSeedPlan plan = batchPlans.get(index);
                messageBatch.addAll(buildMessages(plan, savedConversation));
            }

            aiConversationRepository.saveAll(savedConversations);

            for (int msgStart = 0; msgStart < messageBatch.size(); msgStart += BATCH_SIZE) {
                int msgEnd = Math.min(msgStart + BATCH_SIZE, messageBatch.size());
                List<AIMessage> subBatch = messageBatch.subList(msgStart, msgEnd);
                aiMessageRepository.saveAll(subBatch);
                messageCounter += subBatch.size();
            }
        }

        log.info("Seeded AI conversations={} and messages={}", conversationCounter, messageCounter);
    }

    private boolean isEligibleUser(User user) {
        if (user == null || user.getId() == null || user.getRole() == null) {
            return false;
        }

        UserArchetype archetype = userBehaviorSimulator.getArchetype(user.getId());
        return archetype == UserArchetype.POWER_BUYER || archetype == UserArchetype.REGULAR_BUYER;
    }

    private ConversationSeedPlan buildConversationPlan(User user, List<Product> products) {
        LocalDateTime baseCreatedAt = randomDateTimeWithinLast90Days();
        Role role = user.getRole();

        int turns = ThreadLocalRandom.current().nextInt(3, 9);
        String firstUserMessage = fillTemplate(resolveRandomTemplate(role), products);
        String title = truncate(firstUserMessage, 50);

        AIConversation conversation = AIConversation.builder()
                .userId(user.getId())
                .title(title)
                .userRole(role.name())
                .shopId(role == Role.VENDOR ? resolveVendorShopId(user) : null)
                .messageCount(turns)
                .lastMessage(firstUserMessage)
                .lastMessageAt(baseCreatedAt)
                .isActive(true)
                .createdAt(baseCreatedAt)
                .updatedAt(baseCreatedAt)
                .build();

        return new ConversationSeedPlan(user, conversation, firstUserMessage, turns, baseCreatedAt, products);
    }

    private List<AIMessage> buildMessages(ConversationSeedPlan plan, AIConversation savedConversation) {
        List<AIMessage> messages = new ArrayList<>();

        String lastMessage = plan.firstMessage();
        LocalDateTime lastMessageAt = plan.baseCreatedAt();

        for (int turnIndex = 0; turnIndex < plan.turns(); turnIndex++) {
            boolean isUserTurn = turnIndex % 2 == 0;

            String content;
            if (isUserTurn) {
                if (turnIndex == 0) {
                    content = plan.firstMessage();
                } else {
                    content = fillTemplate(resolveRandomTemplate(plan.user().getRole()), plan.products());
                }
            } else {
                content = faker.lorem().paragraph(2);
            }

            LocalDateTime createdAt = plan.baseCreatedAt().plusMinutes((long) turnIndex * ThreadLocalRandom.current().nextInt(3, 21));

            AIMessage message = AIMessage.builder()
                    .userId(plan.user().getId())
                    .conversationId(savedConversation.getId())
                    .messageType(isUserTurn ? AIMessage.AIMessageType.USER : AIMessage.AIMessageType.AI)
                    .content(content)
                    .userRole(plan.user().getRole().name())
                    .shopId(savedConversation.getShopId())
                    .createdAt(createdAt)
                    .build();

            messages.add(message);
            lastMessage = content;
            lastMessageAt = createdAt;
        }

        savedConversation.setLastMessage(truncate(lastMessage, 100));
        savedConversation.setLastMessageAt(lastMessageAt);
        savedConversation.setUpdatedAt(lastMessageAt);

        return messages;
    }

    private String resolveVendorShopId(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return shopRepository.findByVendorId(user.getId()).map(shop -> shop.getId()).orElse(null);
    }

    private String resolveRandomTemplate(Role role) {
        List<String> templates = switch (role) {
            case VENDOR -> VENDOR_TEMPLATES;
            case ADMIN -> ADMIN_TEMPLATES;
            case USER -> BUYER_TEMPLATES;
        };

        return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
    }

    private String fillTemplate(String template, List<Product> products) {
        String resolved = template;

        String productA = pickProductName(products, "iPhone 15 Pro");
        String productB = pickProductName(products, "Samsung Galaxy S24");
        String productName = pickProductName(products, "MacBook Air M3");
        int priceLimit = ThreadLocalRandom.current().nextInt(10, 51);

        resolved = resolved.replace("[product A]", productA);
        resolved = resolved.replace("[product B]", productB);
        resolved = resolved.replace("[product name]", productName);
        resolved = resolved.replace("[price]", String.valueOf(priceLimit));

        return resolved;
    }

    private String pickProductName(List<Product> products, String fallback) {
        if (products == null || products.isEmpty()) {
            return fallback;
        }

        List<Product> candidates = products.stream()
                .filter(Objects::nonNull)
                .filter(product -> product.getName() != null && !product.getName().isBlank())
                .toList();

        if (candidates.isEmpty()) {
            return fallback;
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())).getName();
    }

    private LocalDateTime randomDateTimeWithinLast90Days() {
        return LocalDateTime.now()
                .minusDays(ThreadLocalRandom.current().nextLong(0, 90))
                .minusHours(ThreadLocalRandom.current().nextInt(0, 24))
                .minusMinutes(ThreadLocalRandom.current().nextInt(0, 60));
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private record ConversationSeedPlan(
            User user,
            AIConversation conversation,
            String firstMessage,
            int turns,
            LocalDateTime baseCreatedAt,
            List<Product> products
    ) {
    }
}
