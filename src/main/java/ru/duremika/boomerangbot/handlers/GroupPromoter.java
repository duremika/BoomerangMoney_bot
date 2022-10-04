package ru.duremika.boomerangbot.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.keyboards.Keyboards;
import ru.duremika.boomerangbot.service.OrderService;
import ru.duremika.boomerangbot.service.TelegramBot;
import ru.duremika.boomerangbot.service.UserService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;

@Slf4j
@Component
public class GroupPromoter {
    private final TelegramBot bot;
    private final UserService userService;
    private final OrderService orderService;
    private final Keyboards keyboards;

    public GroupPromoter(@Lazy TelegramBot bot, UserService userService, OrderService orderService, Keyboards keyboards) {
        this.bot = bot;
        this.userService = userService;
        this.orderService = orderService;
        this.keyboards = keyboards;
    }

    public void mayBeGroupName(Update update) throws TelegramApiException {
        Message message = update.getMessage();
        String usernameOrChatId = getUsernameOrChatIdFromTextMessage(message.getText());
        Chat group;
        try {
            group = bot.execute(new GetChat(usernameOrChatId));
        } catch (TelegramApiException e) {
            bot.execute(chatNotFound(update));
            return;
        }
        log.info("Group in mayBeGroupName method: " + group);
        String link = group.getUserName() != null ?
                "https://t.me/" + group.getUserName() :
                group.getInviteLink();

        if (link == null) {
           bot.execute(botNotAdminGroup(update, group));
        } else {
            Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
            User userDB;
            if (optionalUserDB.isPresent()) {
                userDB = optionalUserDB.get();
            } else {
               bot.execute(SendMessage.builder()
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

           bot.execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("✅ Группа добавлена! ✅\n\n" +
                            "\uD83D\uDCB8 С Вашего баланса списано " + decimalFormat.format(writeOfAmount) + "₽\n\n" +
                            "♻️ В случае выхода пользователя из вашей группы вы получите компенсацию на рекламный баланс в полном размере")
                    .build()

            );

           bot.execute(SendMessage.builder()
                    .text("\uD83D\uDE80 Доступно новое задание на " + lastMessage[1] + " переход")
                    .chatId("-1001697520335")
                    .replyMarkup(keyboards.addChannelToInfoChannelInlineKeyboard())
                    .build());
        }
    }


    public String getUsernameOrChatIdFromTextMessage(String usernameOrChatId) {
        if (usernameOrChatId.startsWith("-100") || usernameOrChatId.startsWith("@")) return usernameOrChatId;
        if (usernameOrChatId.startsWith("100")) return "-" + usernameOrChatId;
        try {
            Long ignored = Long.parseLong(usernameOrChatId);
            return "-100" + usernameOrChatId;
        } catch (NumberFormatException ignored) {
        }
        return "@" + usernameOrChatId;
    }

    public SendMessage chatNotFound(Update update) {
        return SendMessage.builder()
                .text("❗️Ошибка❗️\n\n" +
                        "Проверьте, что вы отправляете USERNAME или CHAT_ID вашей группы!"
                )
                .chatId(update.getMessage().getChatId())
                .build();
    }

    public SendMessage botNotAdminGroup(Update update, Chat group) {
        String type = group != null ? group.getType() : null;
        return SendMessage.builder()
                .text("❗️Ошибка❗️\n\n" +
                        "Проверьте, является ли наш бот администратором Вашей " +
                        ("supergroup".equals(type) ? "супергруппы!" : "группы!")
                )
                .chatId(update.getMessage().getChatId())
                .build();
    }

}
