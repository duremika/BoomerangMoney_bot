package ru.duremika.boomerangbot.annotations;

import lombok.Getter;

public enum ChatType {
    PRIVATE("private"),
    GROUP("group"),
    CHANNEL("channel"),
    SUPERGROUP("supergroup");

    @Getter
    private final String type;

    ChatType(String type) {
        this.type = type;
    }
}
