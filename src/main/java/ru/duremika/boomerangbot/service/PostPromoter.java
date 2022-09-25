package ru.duremika.boomerangbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.duremika.boomerangbot.constants.Keyboards;

import java.text.DecimalFormat;
import java.util.List;

public class PostPromoter {
    static SendMessage viewPostChecker(Message message, String chatId) {
        String callbackData = message.getForwardFromChat().getUserName() + "/" + message.getMessageId();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        float postViewPrice = 0.03f;

        return SendMessage.builder()
                .text("Для начисления нажмите на кнопку:")
                .chatId(chatId)
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCB0 +" + decimalFormat.format(postViewPrice) + "₽") {{
                            setCallbackData(callbackData);
                        }}))
                        .build()
                )
                .build();
    }

    static SendMessage addTaskToInfoChannel(Message message, String amount, String chatId) {
        return SendMessage.builder()
                .text("\uD83D\uDE80 Доступно новое задание на " + amount + " просмотров")
                .chatId(chatId)
                .replyMarkup(Keyboards.addPostToInfoChannelInlineKeyboard)
                .build();
    }
}
