package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.exception.MessageHandlerException;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    TelegramEventsHandler eventsHandler;
    private final Map<String, Method> handlers;

    public TelegramBot(BotConfig config, TelegramEventsHandler eventsHandler) {
        this.config = config;
        eventsHandler.setBot(this);
        this.eventsHandler = eventsHandler;
        handlers = new HashMap<>();
    }

    @PostConstruct
    private void initializeMethods() {
        for (Method method : eventsHandler.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Filter.class)) {
                Filter filter = method.getAnnotation(Filter.class);
                if (filter != null) {
                    for (String value : filter.value()) {
                        handlers.put(value, method);
                    }
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
        try {
            message = recompiledMessage(update);
        } catch (MessageHandlerException e) {
            log.error(e.getMessage());
            return;
        }

        Method method;
        try {
            method = getSuitableMethod(message);
        } catch (MessageHandlerException e) {
            return; // does not require processing
        }


        try {
            Object invoke = method.invoke(eventsHandler, message);
            if (invoke instanceof BotApiMethod) {
                BotApiMethod<? extends Serializable> botApiMethod =
                        (BotApiMethod<? extends Serializable>) invoke;
                log.info("executing: " + botApiMethod);
                execute(botApiMethod);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Message recompiledMessage(Update update) throws MessageHandlerException {
        Message message;
        if (update.hasMessage()) {
            message = update.getMessage();
        } else if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
            message.setText(update.getCallbackQuery().getData());
            return message;
        } else if (update.hasMyChatMember()) {
            message = new Message();
            message.setChat(update.getMyChatMember().getChat());
            message.setText(update.getMyChatMember().getNewChatMember().getStatus());
        } else {
            throw new MessageHandlerException("update was not processed: " + update);
        }
        log.info("text: '" + message.getText() + "' " + message);
        return message;
    }

    private Method getSuitableMethod(Message message) throws MessageHandlerException {
        Method method = handlers.get(message.getText());
        if (method == null) {
            log.warn("for text: '" + message.getText() + "' no suitable method");
            method = handlers.get("error");
        }
        String handlerChatType = method.getAnnotation(Filter.class).chatType().getType();
        String chatType = message.getChat().getType();
        if (!handlerChatType.equals(chatType)) {
            throw new MessageHandlerException();
        }
        return method;
    }

    boolean checkSubscribeToInfoChannel(Long userId) throws TelegramApiException {
        return execute(new GetChatMember(config.getInfoChannelId(), userId)).getStatus().equals("left");
    }

    boolean checkProfilePhoto(Long userId) throws TelegramApiException {
        return execute(new GetUserProfilePhotos(userId, 0, 1)).getTotalCount() == 0;
    }

    boolean checkSubscribeToViewsChannel(Long userId) throws TelegramApiException {
        return execute(new GetChatMember(config.getViewsChannelId(), userId)).getStatus().equals("left");
    }
}
