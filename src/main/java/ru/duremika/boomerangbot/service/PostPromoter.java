package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.duremika.boomerangbot.keyboards.Keyboards;

@Component
@RequiredArgsConstructor
public class PostPromoter {
    private final Keyboards keyboards;


    public SendMessage viewPostChecker(String chatId, Long orderId) {
        return SendMessage.builder()
                .text("Для начисления нажмите на кнопку:")
                .chatId(chatId)
                .replyMarkup(keyboards.postViewedInlineKeyboard(orderId))
                .build();
    }

    public SendMessage addTaskToInfoChannel(String amount, String chatId) {
        return SendMessage.builder()
                .text("\uD83D\uDE80 Доступно новое задание на " + amount + " просмотров")
                .chatId(chatId)
                .replyMarkup(keyboards.addPostToInfoChannelInlineKeyboard())
                .build();
    }
}
