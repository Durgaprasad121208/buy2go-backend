package com.buy2go.api.utils;

import java.util.UUID;

public class OrderNumberGenerator {

    public static String generateOrderNumber() {
        // Formats like #9a4c3517
        String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "#" + hex;
    }

    public static String generateSlug(String title) {
        if (title == null) return UUID.randomUUID().toString();
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
