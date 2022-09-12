package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.constants.Keyboards;

@Component
public class TelegramEventsHandler {
    private final UserService userService;

    public TelegramEventsHandler(UserService userService) {
        this.userService = userService;
    }


    void welcome(TelegramBot bot, Update update) {
        SendMessage sendMessage;
        Long id = update.getMyChatMember().getChat().getId();

        boolean isNewUser = userService.createOrUpdateUser(id);
        String text = isNewUser ? "✅ Отлично!\nВы зарегистрированы!" : "✋ С возвращением";
        sendMessage = SendMessage.builder()
                .chatId(id)
                .text(text)
                .replyMarkup(Keyboards.mainReplyKeyboardMarkup)
                .build();
        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    void goodbye( Update update) {
        Long id = update.getMyChatMember().getChat().getId();
        userService.disableUser(id);
    }
}
