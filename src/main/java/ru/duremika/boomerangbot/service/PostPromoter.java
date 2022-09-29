package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.keyboards.Keyboards;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostPromoter {
    private final BotConfig config;
    private final Keyboards keyboards;

    private DecimalFormat decimalFormat;
    private float postViewPrice;

    @PostConstruct
    private void init(){
            decimalFormat = config.getDecimalFormat();
            postViewPrice = config.getPostViewPrice();
    }


    public SendMessage viewPostChecker(Message message, String chatId) {
        String channelName = message.getForwardFromChat().getUserName();
        if (channelName == null) channelName = "c/" + String.valueOf(message.getForwardFromChat().getId()).substring(4);
        String callbackData = channelName + "/" + message.getForwardFromMessageId();
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

    public SendMessage addTaskToInfoChannel(String amount, String chatId) {
        return SendMessage.builder()
                .text("\uD83D\uDE80 Доступно новое задание на " + amount + " просмотров")
                .chatId(chatId)
                .replyMarkup(keyboards.addPostToInfoChannelInlineKeyboard)
                .build();
    }
}
