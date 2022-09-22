package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Chat;
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
    private final UserService userService;
    TelegramEventsHandler eventsHandler;
    private final Map<String, Method> handlers;

    public TelegramBot(BotConfig config, UserService userService, TelegramEventsHandler eventsHandler) {
        this.config = config;
        this.userService = userService;
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
                String text = message.getText();
                log.info("executing: " + botApiMethod);
                execute(botApiMethod);

                if (tryParseInt(text) != null) {
                    String lastMessage = userService.getLastMessage(message.getChatId());
                    userService.saveLastMessage(message.getChatId(), lastMessage + " " + text);
                } else  {
                    log.info(message.toString());
                    log.info(update.toString());
                    Chat chat = message.getChat();
                    Long id = chat != null && chat.getType().equals("private") ? message.getChatId() : message.getFrom().getId();
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
            if (message.getText() == null) {
                if (message.getForwardFromChat() != null) {
                    message.setText("forward");
                }
            }
        } else if (update.hasCallbackQuery()) {
            message = update.getCallbackQuery().getMessage();
            message.setText(update.getCallbackQuery().getData());
            if (message.getText().contains("/")){
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
        log.info("text: '" + message.getText() + "' " + message);
        return message;
    }

    private Method getSuitableMethod(Message message) throws MessageHandlerException {
        String text = message.getText();
        Method method = handlers.get(text);
        if (method == null) {
            Integer number = tryParseInt(text);
            if (number != null) {
                method = handlers.get("number");
            } else if (text.contains("/")) {
                method = handlers.get("post_viewed");
            }else {
                log.warn("for text: '" + text + "' no suitable method");
                method = handlers.get("error");
            }
        }
        String handlerChatType = method.getAnnotation(Filter.class).chatType().getType();
        String chatType = message.getChat().getType();
        if (!handlerChatType.equals(chatType)) {
            throw new MessageHandlerException();
        }
        return method;
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
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

    public void promotePosts(Message message, String amount) {
        String viewsChannelId = config.getViewsChannelId();
        String infoChannelId = config.getInfoChannelId();

        String fromChatId = message.getChatId().toString();
        Integer messageId = message.getMessageId();

        try {
            execute(new ForwardMessage(viewsChannelId, fromChatId, messageId));
            execute(eventsHandler.viewPostChecker(message, viewsChannelId));
            execute(eventsHandler.addTaskToInfoChannel(message, amount, infoChannelId));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
