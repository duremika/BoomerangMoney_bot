package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.duremika.boomerangbot.constants.Keyboards;

@Component
public class TelegramEventsHandler {
    private final UserService userService;

    public TelegramEventsHandler(UserService userService) {
        this.userService = userService;
    }


    SendMessage welcome(Long id) {
        boolean isNewUser = userService.createOrUpdateUser(id);
        String text = isNewUser ? "✅ Отлично!\nВы зарегистрированы!" : "✋ С возвращением";
        return SendMessage.builder()
                .chatId(id)
                .text(text)
                .replyMarkup(Keyboards.mainReplyKeyboardMarkup)
                .build();
    }

    void goodbye(Long id) {
        userService.disableUser(id);
    }
}
