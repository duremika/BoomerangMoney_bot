package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.duremika.boomerangbot.keyboards.Keyboards;

@Component
@RequiredArgsConstructor
public class ChannelPromoter {
    private final Keyboards keyboards;


    public SendMessage addTaskToInfoChannel(String amount, String chatId) {
        return SendMessage.builder()
                .text("\uD83D\uDE80 Доступно новое задание на " + amount + " подписок")
                .chatId(chatId)
                .replyMarkup(keyboards.addChannelToInfoChannelInlineKeyboard)
                .build();
    }
}
