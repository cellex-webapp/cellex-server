package com.example.cellex.enums;

import java.util.List;
import java.util.stream.Collectors;

public enum VendorPermission {
    PRODUCT_VIEW("PRODUCT:VIEW"),
    PRODUCT_CREATE("PRODUCT:CREATE"),
    PRODUCT_UPDATE("PRODUCT:UPDATE"),
    PRODUCT_DELETE("PRODUCT:DELETE"),
    ORDER_VIEW("ORDER:VIEW"),
    ORDER_CONFIRM("ORDER:CONFIRM"),
    ORDER_SHIP("ORDER:SHIP"),
    INVENTORY_VIEW("INVENTORY:VIEW"),
    INVENTORY_IMPORT("INVENTORY:IMPORT"),
    INVENTORY_CHECK("INVENTORY:CHECK"),
    SUPPLIER_VIEW("SUPPLIER:VIEW"),
    SUPPLIER_CREATE("SUPPLIER:CREATE"),
    SUPPLIER_UPDATE("SUPPLIER:UPDATE"),
    REVIEW_VIEW("REVIEW:VIEW"),
    REVIEW_RESPOND("REVIEW:RESPOND"),
    CHAT_VIEW("CHAT:VIEW"),
    ANALYTICS_VIEW("ANALYTICS:VIEW"),
    SHOP_UPDATE("SHOP:UPDATE"),
    SHOP_THEME_MANAGE("SHOP_THEME:MANAGE"),
    LIVESTREAM_CREATE("LIVESTREAM:CREATE"),
    LIVESTREAM_MANAGE("LIVESTREAM:MANAGE");

    private final String key;

    VendorPermission(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static List<String> allKeys() {
        return java.util.Arrays.stream(values()).map(VendorPermission::key).collect(Collectors.toList());
    }
}

