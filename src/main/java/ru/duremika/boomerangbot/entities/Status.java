package ru.duremika.boomerangbot.entities;

import lombok.Getter;

public enum Status {
    INACTIVE("Не активный"),
    EXECUTOR("Исполнитель"),
    ADVERTISER("Рекламодатель"),
    PLAYER("Игрок"),
    BANNED("Заблокирован");

    @Getter
    private final String title;

    Status(String title) {
        this.title = title;
    }
}
