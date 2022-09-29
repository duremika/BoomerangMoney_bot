package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.common.MessageType;
import ru.duremika.boomerangbot.config.BotConfig;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserService userService;
    private final List<Handler> handlerClasses;

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
        log.info(update.toString().replace("\n", "\\n"));
        MessageType messageType = null;
        Map.Entry<Method, Handler> selectedMethod = null;
        String text = null;
        String command = null;
        String callback = null;
        String newLastMessage = null;
        Class<? extends ChatMember> chatMember = null;
        Filter.ChatType chatType = null;
        Long userId = null;

        if (update.hasMessage() && update.getMessage().getForwardFromChat() != null) {
            messageType = MessageType.FORWARD;
            newLastMessage = "forward";
            chatType = Filter.ChatType.fromString(update.getMessage().getChat().getType());
            userId = update.getMessage().getFrom().getId();
        } else if (update.hasMessage() && update.getMessage().isCommand()) {
            messageType = MessageType.COMMAND;
            command = update.getMessage().getText();
            newLastMessage = command;
            chatType = Filter.ChatType.fromString(update.getMessage().getChat().getType());
            userId = update.getMessage().getFrom().getId();
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            messageType = MessageType.TEXT;
            text = update.getMessage().getText();
            newLastMessage = text;
            chatType = Filter.ChatType.fromString(update.getMessage().getChat().getType());
            userId = update.getMessage().getFrom().getId();
        } else if (update.hasCallbackQuery()) {
            messageType = MessageType.CALLBACK;
            callback = update.getCallbackQuery().getData();
            newLastMessage = callback;
            chatType = Filter.ChatType.fromString(update.getCallbackQuery().getMessage().getChat().getType());
            userId = update.getCallbackQuery().getFrom().getId();
        } else if (update.hasMyChatMember()) {
            messageType = MessageType.CHAT_MEMBER_UPDATED;
            chatMember = update.getMyChatMember().getNewChatMember().getClass();
            chatType = Filter.ChatType.fromString(update.getMyChatMember().getChat().getType());
            userId = update.getMyChatMember().getFrom().getId();
        }
        log.info("Request | type: " + messageType +
                ", text: " + (text != null ? text.replace("\n", "\\n") : null) +
                ", command: " + command +
                ", callback: " + callback +
                ", chatMember: " + chatMember +
                ", chatType: " + chatType +
                ", userId: " + userId);

        if (chatType == null) {
            log.error("Unknown chat type in update: " + update);
            return;
        }

        loop:
        for (Handler handler : handlerClasses) {
            for (Method method : handler.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Filter.class)) {
                    Filter filter = method.getAnnotation(Filter.class);
                    if (!filter.chatType().equals(chatType)) {
                        continue;
                    }
                    if (Arrays.asList(filter.text()).contains(text)) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.specialType()).contains("number")
                            && text != null && tryParseInt(text) != null) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.command()).contains(command)) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.callback()).contains(callback)) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.specialType()).contains("forward") &&
                            MessageType.FORWARD.equals(messageType)) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.specialType()).contains("post_viewed") &&
                            MessageType.CALLBACK.equals(messageType) && callback.contains("/")) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.chatMemberUpdated()).contains(chatMember)) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                }
            }
        }
        if (selectedMethod != null) {
            methodInvoke(update, selectedMethod);
        } else {
            log.error("Failed to select method");
            try {
                execute(error(update));
            } catch (TelegramApiException ignored) {
            }
        }

        updateLastMessage(userId, newLastMessage);
    }

    private void methodInvoke(Update update, Map.Entry<Method, Handler> selectedMethod) {
        try {
            selectedMethod.getKey().setAccessible(true);
            log.info("Selected method: " + selectedMethod.getKey().getName() + " in Handler: " + selectedMethod.getValue().getClass().getName());
            Object invoke = selectedMethod.getKey().invoke(selectedMethod.getValue(), update);
            if (invoke instanceof BotApiMethod) {
                BotApiMethod<? extends Serializable> botApiMethod =
                        (BotApiMethod<? extends Serializable>) invoke;
                var message = execute(botApiMethod);
                log.info("Response: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLastMessage(Long userId, String newLastMessage) {
        if (tryParseInt(newLastMessage) != null) {
            String lastMessage = userService.getLastMessage(userId);
            userService.saveLastMessage(userId, lastMessage + " " + newLastMessage);
        } else {
            userService.saveLastMessage(userId, newLastMessage);
        }
    }

    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean checkSubscribeToInfoChannel(Long userId) throws TelegramApiException {
        return execute(new GetChatMember(config.getInfoChannelId(), userId)).getStatus().equals("left");
    }

    public boolean checkProfilePhoto(Long userId) throws TelegramApiException {
        return execute(new GetUserProfilePhotos(userId, 0, 1)).getTotalCount() == 0;
    }

    public boolean checkSubscribeToViewsChannel(Long userId) throws TelegramApiException {
        return execute(new GetChatMember(config.getViewsChannelId(), userId)).getStatus().equals("left");
    }

    public SendMessage error(Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        Long chatId = message.getChatId();
        return SendMessage.builder()
                .chatId(chatId)
                .text("\uD83E\uDD28")
                .build();
    }
}
