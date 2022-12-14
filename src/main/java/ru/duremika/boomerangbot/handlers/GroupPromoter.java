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
import ru.duremika.boomerangbot.config.BotConfig;
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
    private final DecimalFormat decimalFormat;
    private final Keyboards keyboards;
    private final float groupOrderPrice;
    private final String infoChannelId;

    public GroupPromoter(@Lazy TelegramBot bot, BotConfig config, UserService userService, OrderService orderService, Keyboards keyboards) {
        this.bot = bot;
        this.userService = userService;
        this.orderService = orderService;
        this.keyboards = keyboards;


        decimalFormat = config.getDecimalFormat();
        groupOrderPrice = config.getGroupOrderPrice();
        infoChannelId = config.getInfoChannelId();
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
            addGroupOrder(message, usernameOrChatId);
        }
    }

    private void addGroupOrder(Message message, String link) throws TelegramApiException{
        int amount = Integer.parseInt(userService.getLastMessage(message.getChatId()).split(" ")[1]);
        float writeOfAmount = amount * groupOrderPrice;

        Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
        User userDB;
        if (optionalUserDB.isPresent()) {
            userDB = optionalUserDB.get();
            float advertisingBalance = userDB.getBalance().getAdvertising();
            if (advertisingBalance < writeOfAmount) {
                bot.execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("?????? ???????????????????????? ?????????????? ???? ??????????????!\n" +
                                "???? ?????????????? " + decimalFormat.format(writeOfAmount - advertisingBalance) + " ???")
                        .build());
                return;
            }
        } else {
            bot.execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("?????? ???? ?????????? ???? ??????. ???????????????????? ?????????????????????????? ????????")
                    .build());
            return;
        }

        if (amount >= 5000) {
            amount += 1000;
        } else if (amount >= 2000) {
            amount += 300;
        } else if (amount >= 1000) {
            amount += 100;
        } else if (amount >= 500) {
            amount += 25;
        }

        Order order = new Order();
        order.setLink(link);
        order.setAuthor(userDB);
        order.setAmount(amount);
        order.setType(Order.Type.GROUP);
        order.setTasks(new ArrayList<>());

        orderService.add(order);

        userService.writeOfFromAdvertising(userDB.getId(), writeOfAmount);

        bot.execute(SendMessage.builder()
                .chatId(message.getChatId())
                .text("??? ???????????? ??????????????????! ???\n\n" +
                        "\uD83D\uDCB8 ?? ???????????? ?????????????? ?????????????? " + decimalFormat.format(writeOfAmount) + "???\n\n" +
                        "?????? ?? ???????????? ???????????? ???????????????????????? ???? ?????????? ???????????? ???? ???????????????? ?????????????????????? ???? ?????????????????? ???????????? ?? ???????????? ??????????????")
                .build()

        );

        bot.execute(SendMessage.builder()
                .text("\uD83D\uDE80 ???????????????? ?????????? ?????????????? ???? " + amount + " ??????????????")
                .chatId(infoChannelId)
                .replyMarkup(keyboards.addChannelToInfoChannelInlineKeyboard())
                .build());
    }


    private String getUsernameOrChatIdFromTextMessage(String usernameOrChatId) {
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
                .text("????????????????????????\n\n" +
                        "??????????????????, ?????? ???? ?????????????????????? USERNAME ?????? CHAT_ID ?????????? ????????????!"
                )
                .chatId(update.getMessage().getChatId())
                .build();
    }

    private SendMessage botNotAdminGroup(Update update, Chat group) {
        String type = group != null ? group.getType() : null;
        return SendMessage.builder()
                .text("????????????????????????\n\n" +
                        "??????????????????, ???????????????? ???? ?????? ?????? ?????????????????????????????? ?????????? " +
                        ("supergroup".equals(type) ? "??????????????????????!" : "????????????!")
                )
                .chatId(update.getMessage().getChatId())
                .build();
    }

}
