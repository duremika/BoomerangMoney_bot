package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.duremika.boomerangbot.config.BotConfig;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    final TelegramEventsHandler eventsHandler;

    public TelegramBot(BotConfig config, TelegramEventsHandler eventsHandler) {
        this.config = config;
        this.eventsHandler = eventsHandler;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("member")) {
            eventsHandler.welcome(this, update);
        } else if (update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
            eventsHandler.goodbye(update);
        } else {
            log.info(update.toString());
        }
    }
}
