package com.example.cellex.config;

import java.util.HashSet;
import java.util.Set;

public class TrimContext {
    private static final ThreadLocal<Set<String>> TL = ThreadLocal.withInitial(HashSet::new);

    public static void mark(String field) {
        TL.get().add(field);
    }

    public static boolean contains(String field) {
        return TL.get().contains(field);
    }

    public static void clear() {
        TL.get().clear();
    }
}
