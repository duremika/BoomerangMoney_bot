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
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.exception.MessageHandlerException;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserService userService;
    private final List<Handler> handlerClasses;
    private final Map<String, Map<Method, Handler>> handlers;

    public TelegramBot(BotConfig config, UserService userService, List<Handler> handlerClasses) {
        this.config = config;
        this.userService = userService;
        this.handlerClasses = handlerClasses;
        handlers = new HashMap<>();
    }

    @PostConstruct
    private void initializeMethods() {
        for (Handler handler : handlerClasses) {
            for (Method method : handler.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Filter.class)) {
                    Filter filter = method.getAnnotation(Filter.class);
                    if (filter != null) {
                        for (String value : filter.value()) {
                            handlers.put(value, Collections.singletonMap(method, handler));
                        }
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

        Map.Entry<Method, Handler> methodClassEntry;
        try {
            methodClassEntry = getSuitableMethod(message);
        } catch (MessageHandlerException e) {
            return; // does not require processing
        }


        try {
            Object invoke = methodClassEntry.getKey().invoke(methodClassEntry.getValue(), message);
            if (invoke instanceof BotApiMethod) {
                BotApiMethod<? extends Serializable> botApiMethod =
                        (BotApiMethod<? extends Serializable>) invoke;
                String text = message.getText();
                log.info("executing: " + botApiMethod);
                execute(botApiMethod);

                if (tryParseInt(text) != null) {
                    String lastMessage = userService.getLastMessage(message.getChatId());
                    userService.saveLastMessage(message.getChatId(), lastMessage + " " + text);
                } else {
                    log.info(message.toString());
                    log.info(update.toString());
                    Chat chat = message.getChat();
                    Long id =
                            chat != null && chat.getType().equals("private") ?
                                    message.getChatId() :
                                    message.getFrom().getId();
                    userService.saveLastMessage(id, text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Message recompiledMessage(Update update) throws MessageHandlerException {
        Message message;
        if (update.hasMessage()) {
            message = update.getMessage();
            if (message.getForwardFromChat() != null) {
                message.setText("forward");

            }
        } else if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
            message.setText(update.getCallbackQuery().getData());
            if (message.getText().contains("/")) {
                message.setFrom(update.getCallbackQuery().getFrom());
                message.setCaption(update.getCallbackQuery().getId()); // TODO need refactoring
            }
        } else if (update.hasMyChatMember()) {
            message = new Message();
            message.setChat(update.getMyChatMember().getChat());
            message.setText(update.getMyChatMember().getNewChatMember().getStatus());
        } else {
            throw new MessageHandlerException("update was not processed: " + update);
        }
        message.setEntities(null);
        log.info("text: '" + message.getText() + "' " + message);
        return message;
    }

    private Map.Entry<Method, Handler> getSuitableMethod(Message message) throws MessageHandlerException {
        String text = message.getText();

        Map<Method, Handler> methodClassMap = handlers.get(text);
        Map.Entry<Method, Handler> entry = methodClassMap == null ?
                defaultMethod(text).entrySet()
                        .stream()
                        .findFirst()
                        .orElseThrow() :
                methodClassMap.entrySet()
                        .stream()
                        .findFirst()
                        .orElseThrow();

        Method method = entry.getKey();

        String handlerChatType = method.getAnnotation(Filter.class).chatType().getType();
        String chatType = message.getChat().getType();
        if (!handlerChatType.equals(chatType)) {
            throw new MessageHandlerException();
        }
        return entry;
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<Method, Handler> defaultMethod(String text) {
        Integer number = tryParseInt(text);
        if (number != null) {
            return handlers.get("number");
        } else if (text.contains("/")) {
            return handlers.get("post_viewed");
        } else {
            log.warn("for text: '" + text + "' no suitable method");
            return handlers.get("error");
        }
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
