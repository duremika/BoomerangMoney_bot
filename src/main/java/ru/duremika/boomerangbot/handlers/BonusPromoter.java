package ru.duremika.boomerangbot.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.keyboards.Keyboards;
import ru.duremika.boomerangbot.service.TelegramBot;

@Slf4j
@Component
public class BonusPromoter {
    private final TelegramBot bot;
    private final Keyboards keyboards;

    public BonusPromoter(@Lazy TelegramBot bot, Keyboards keyboards) {
        this.bot = bot;
        this.keyboards = keyboards;
    }


    public void representBonusText(Update update) throws TelegramApiException {
        Message message = update.getMessage();
        String chatId = String.valueOf(message.getChatId());

        if (message.getText().getBytes().length > 1950) {
            bot.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Слишком длинный текст")
                    .replyMarkup(keyboards.longBonusTextInlineKeyboard())
                    .build());
        }

        String text = "Текст будет выглядить так:⤵\n\n\uD83D\uDCB8 Ежедневный бонус \uD83D\uDCB8\n\n" +
                "Спонсор бонуса:\n\n" + message.getText() + "\n\n✅ Бонус доступен! ✅";
        bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboards.representBonusTextInlineKeyboard())
                .build());

    }
}
