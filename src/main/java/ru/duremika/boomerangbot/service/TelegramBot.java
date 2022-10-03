package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.common.MessageType;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.keyboards.Keyboards;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {
    private final BotConfig config;
    private final UserService userService;
    private final List<Handler> handlerClasses;
    private final Keyboards keyboards;
    private final OrderService orderService;

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
                            && text != null && tryParseLong(text) != null) {

                        String lm = userService.getLastMessage(update.getMessage().getChatId());
                        String[] lmarr;
                        if (lm != null && (lmarr = lm.split(" ")).length == 2 &&
                                lmarr[0].equals("add_group") && tryParseLong(lmarr[1]) != null) {
                            try {
                                mayBeGroupName(update);
                            } catch (TelegramApiException ignored) {
                            }
                            return;
                        }
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
                            MessageType.CALLBACK.equals(messageType) && callback.contains("post_viewed")) {
                        selectedMethod = Map.entry(method, handler);
                        break loop;
                    }
                    if (Arrays.asList(filter.specialType()).contains("check_subscribe") &&
                            MessageType.CALLBACK.equals(messageType) && callback.contains("check_subscribe")) {
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
            String lm = update.hasMessage() ? userService.getLastMessage(update.getMessage().getChatId()) : null;
            String[] lmarr;
            if (MessageType.TEXT.equals(messageType) && lm != null && (lmarr = lm.split(" ")).length == 2 &&
                    lmarr[0].equals("add_group") && tryParseLong(lmarr[1]) != null) {
                try {
                    mayBeGroupName(update);
                } catch (TelegramApiException ignored) {
                }
                return;
            }


            log.error("Failed to select method");
            try {
                execute(error(update));
            } catch (TelegramApiException ignored) {
            }
        }
        if (newLastMessage != null) {
            updateLastMessage(userId, newLastMessage);
        }
    }


    private void mayBeGroupName(Update update) throws TelegramApiException {
        String usernameOrChatId = getUsernameOrChatId(update.getMessage().getText());
        Chat group;
        try {
            group = execute(new GetChat(usernameOrChatId));
        } catch (TelegramApiException e) {
            execute(chatNotFound(update));
            return;
        }
        log.info("Group in mayBeGroupName method: " + group);
        String link = group.getUserName() != null ?
                "https://t.me/" + group.getUserName() :
                group.getInviteLink();

        if (link == null) {
            execute(botNotAdminGroup(update, group));
        } else {
            Message message = update.getMessage();

            Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
            User userDB;
            if (optionalUserDB.isPresent()) {
                userDB = optionalUserDB.get();
            } else {
                execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("Что то пошло не так. Попробуйте перезапустить бота")
                        .build());
                return;
            }
            String[] lastMessage = userDB.getLastMessage().split(" ");


            Order order = new Order();
            order.setLink(link);
            order.setAuthor(userDB);
            order.setAmount(Integer.parseInt(lastMessage[1]));
            order.setType(Order.Type.GROUP);
            order.setTasks(new ArrayList<>());

            orderService.add(order);
            float writeOfAmount = 0.4f;
            DecimalFormat decimalFormat = new DecimalFormat("0.00");

            userService.writeOfFromAdvertising(userDB.getId(), writeOfAmount);

            execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("✅ Группа добавлена! ✅\n\n" +
                            "\uD83D\uDCB8 С Вашего баланса списано " + decimalFormat.format(writeOfAmount) + "₽\n\n" +
                            "♻️ В случае выхода пользователя из вашей группы вы получите компенсацию на рекламный баланс в полном размере")
                    .build()

            );

            execute(SendMessage.builder()
                    .text("\uD83D\uDE80 Доступно новое задание на " + lastMessage[1] + " переход")
                    .chatId("-1001697520335")
                    .replyMarkup(keyboards.addChannelToInfoChannelInlineKeyboard())
                    .build());
        }
    }

    private String getUsernameOrChatId(String usernameOrChatId) {
        if (usernameOrChatId.startsWith("-100") || usernameOrChatId.startsWith("@")) return usernameOrChatId;
        if (usernameOrChatId.startsWith("100")) return "-" + usernameOrChatId;
        try {
            Long ignored = Long.parseLong(usernameOrChatId);
            return "-100" + usernameOrChatId;
        } catch (NumberFormatException ignored) {
        }
        return "@" + usernameOrChatId;
    }

    private SendMessage chatNotFound(Update update) {
        return SendMessage.builder()
                .text("❗️Ошибка❗️\n\n" +
                        "Проверьте, что вы отправляете USERNAME или CHAT_ID вашей группы!"
                )
                .chatId(update.getMessage().getChatId())
                .build();
    }

    private SendMessage botNotAdminGroup(Update update, Chat group) {
        String type = group != null ? group.getType() : null;
        return SendMessage.builder()
                .text("❗️Ошибка❗️\n\n" +
                        "Проверьте, является ли наш бот администратором Вашей " +
                        ("supergroup".equals(type) ? "супергруппы!" : "группы!")
                )
                .chatId(update.getMessage().getChatId())
                .build();
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
                log.info("Response: " + message.toString().replace("\n", "\\n"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLastMessage(Long userId, String newLastMessage) {
        if (tryParseLong(newLastMessage) != null) {
            String lastMessage = userService.getLastMessage(userId);
            userService.saveLastMessage(userId, lastMessage + " " + newLastMessage);
        } else {
            userService.saveLastMessage(userId, newLastMessage);
        }
    }

    private Long tryParseLong(String text) {
        try {
            return Long.parseLong(text);
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
