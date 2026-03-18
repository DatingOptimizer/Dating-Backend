package com.groupf.dating.common;

import lombok.Getter;

@Getter
public enum ToneType {
    CASUAL("casual", "friendly, relaxed, and conversational"),
    BOLD("bold", "confident, direct, and attention-grabbing"),
    POLITE("polite", "respectful, courteous, and warm"),
    HUMOROUS("humorous", "funny, playful, and light-hearted — use wit and clever humor naturally"),
    WARM("warm", "sincere, empathetic, and heartfelt — speak with genuine emotion and warmth");

    private final String value;
    private final String description;

    ToneType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ToneType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return CASUAL; // Default
        }
        for (ToneType tone : ToneType.values()) {
            if (tone.value.equalsIgnoreCase(value.trim())) {
                return tone;
            }
        }
        throw new IllegalArgumentException(value + " is not a valid ToneType, Acceptable values are: casual, bold, polite, humorous, warm");
    }
}