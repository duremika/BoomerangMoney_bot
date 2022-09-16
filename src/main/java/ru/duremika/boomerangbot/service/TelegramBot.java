package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.config.BotConfig;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    final TelegramEventsHandler eventsHandler;
    final String infoChannelId = "-1001697520335";
    final String viewerChannelId = "-1001718302900";

    private final Map<String, Method> handlers = new HashMap<>();

    public TelegramBot(BotConfig config, TelegramEventsHandler eventsHandler) {
        this.config = config;
        this.eventsHandler = eventsHandler;
    }

    @PostConstruct
    private void initializeMethods() {
        for (Method method : eventsHandler.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Filter.class)) {
                Filter filter = method.getAnnotation(Filter.class);
                if (filter != null) {
                    handlers.put(filter.value(), method);
                }
            }

        }
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
        final Message message;
        final Chat chat;
        final String text;

        if (update.hasMessage()) {
            message = update.getMessage();
            chat = message.getChat();
            text = message.getText();
        } else if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
            chat = message.getChat();
            text = update.getCallbackQuery().getData();
        } else {
            log.warn("Update was not processed:\n" + update);
            return;
        }
        log.info("text: '" + text + "' " + message);

        // TODO: need refactoring
        if (message.getChat().getType().equals("private")) {
            if ((update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("member")) ||
                    update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {
                try {
                    Long id = update.getMyChatMember().getChat().getId();
                    execute(eventsHandler.welcome(id));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
                Long id = update.getMyChatMember().getChat().getId();
                eventsHandler.goodbye(id);
            } else if (message.getText().contains("\uD83D\uDC68\u200D\uD83D\uDCBB")) {
                Long uid = message.getFrom().getId();
                Long chatId = message.getChatId();
                try {
                    if (execute(new GetChatMember(infoChannelId, uid)).getStatus().equals("left")) {
                        execute(eventsHandler.notInInfoChannel(chatId));
                    } else if (execute(new GetUserProfilePhotos(uid, 0, 1)).getTotalCount() == 0) {
                        execute(eventsHandler.hasNotPhoto(chatId));
                    } else if (update.getMessage().getFrom().getUserName() == null) {
                        execute(eventsHandler.hasNotUsername(chatId));
                    } else if (execute(new GetChatMember(viewerChannelId, uid)).getStatus().equals("left")) {
                        execute(eventsHandler.notInViewerChannel(chatId));
                    } else {
                        execute(eventsHandler.captcha(chatId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        Method method = handlers.get(text);
        if (method == null) {
            log.warn("text: '" + text + "' not processed");
            method = handlers.get("error");
        }
        String handlerChatType = method.getAnnotation(Filter.class).chatType().getType();
        if (!handlerChatType.equals(chat.getType())) return;
        try {
            BotApiMethod<? extends Serializable> botApiMethod = (BotApiMethod<? extends Serializable>) method.invoke(eventsHandler, message);
            log.info(String.valueOf(botApiMethod));
            execute(botApiMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
