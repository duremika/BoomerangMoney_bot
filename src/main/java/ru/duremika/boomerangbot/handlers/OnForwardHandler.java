package ru.duremika.boomerangbot.handlers;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.keyboards.Keyboards;
import ru.duremika.boomerangbot.service.OrderService;
import ru.duremika.boomerangbot.service.TaskService;
import ru.duremika.boomerangbot.service.TelegramBot;
import ru.duremika.boomerangbot.service.UserService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class OnForwardHandler implements Handler {
    private final TelegramBot bot;
    private final UserService userService;
    private final OrderService orderService;
    private final TaskService taskService;
    private final PostPromoter postPromoter;
    private final ChannelPromoter channelPromoter;
    private final Keyboards keyboards;

    private final DecimalFormat decimalFormat;
    private final String viewsChannelId;
    private final String infoChannelId;

    private final float postOrderPrice;
    private final float channelOrderPrice;
    private final float botStartPrice;

    public OnForwardHandler(@Lazy TelegramBot bot, BotConfig config, UserService userService, OrderService orderService, TaskService taskService, PostPromoter postPromoter, ChannelPromoter channelPromoter, Keyboards keyboards) {
        this.bot = bot;
        this.userService = userService;
        this.orderService = orderService;
        this.taskService = taskService;
        this.postPromoter = postPromoter;
        this.channelPromoter = channelPromoter;
        this.keyboards = keyboards;

        postOrderPrice = config.getPostOrderPrice();
        channelOrderPrice = config.getChannelOrderPrice();
        botStartPrice = config.getBotStartPrice();


        decimalFormat = config.getDecimalFormat();
        infoChannelId = config.getInfoChannelId();
        viewsChannelId = config.getViewsChannelId();
    }


    @Filter(specialType = "forward")
    public SendMessage forward(Update update) {
        Message message = update.getMessage();
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");

        if ("earn_bot".equals(lastMessage[0])) {
            return onForwardBotMessage(update);
        }
        if (lastMessage.length != 2 || lastMessage[0] == null || lastMessage[1] == null) {
            return bot.error(update);
        }
        int amount = Integer.parseInt(lastMessage[1]);
        switch (lastMessage[0]) {
            case "add_post":
                return promotePosts(update, amount);
            case "add_channel":
                return promoteChannel(message, amount);
            default:
                return bot.error(update);
        }
    }

    private SendMessage promotePosts(Update update, int amount) {
        Message message = update.getMessage();
        String channelName = message.getForwardFromChat().getUserName();
        if (channelName == null) channelName = "c/" + String.valueOf(message.getForwardFromChat().getId()).substring(4);
        String callbackData = channelName + "/" + message.getForwardFromMessageId();

        float writeOfAmount = amount * postOrderPrice;

        String fromChatId = message.getChatId().toString();
        Integer messageId = message.getMessageId();

        Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
        User userDB;
        if (optionalUserDB.isPresent()) {
            userDB = optionalUserDB.get();
        } else {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("?????? ???? ?????????? ???? ??????. ???????????????????? ?????????????????????????? ????????")
                    .build();
        }

        if (amount >= 10000) {
            amount += 2000;
        } else if (amount >= 5000) {
            amount += 750;
        } else if (amount >= 2000) {
            amount += 200;
        } else if (amount >= 1000) {
            amount += 50;
        }

        Order order = new Order();
        order.setLink(callbackData);
        order.setAuthor(userDB);
        order.setAmount(amount);
        order.setType(Order.Type.POST);
        order.setTasks(new ArrayList<>());

        userDB.getOrders().add(order);
        order = orderService.add(order);

        try {
            bot.execute(new ForwardMessage(viewsChannelId, fromChatId, messageId));
            bot.execute(postPromoter.viewPostChecker(viewsChannelId, order.getId()));
            bot.execute(postPromoter.addTaskToInfoChannel(String.valueOf(amount), infoChannelId));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    if (advertisingBalance < writeOfAmount) {
                        sendMessageBuilder
                                .text("?????? ???????????????????????? ?????????????? ???? ??????????????!\n" +
                                        "???? ?????????????? " + decimalFormat.format(writeOfAmount - advertisingBalance) + " ???");
                        return;
                    }
                    userService.writeOfFromAdvertising(user.getId(), writeOfAmount);
                    String text = "??? ???????? ????????????????! ???\n\n" +
                            "\uD83D\uDCB8 ?? ???????????? ?????????????? ?????????????? " + decimalFormat.format(writeOfAmount) + "???";

                    sendMessageBuilder
                            .text(text);
                },
                () -> sendMessageBuilder.text("?????? ???? ?????????? ???? ??????. ???????????????????? ?????????????????????????? ????????")
        );
        return sendMessageBuilder.build();
    }

    private SendMessage promoteChannel(Message message, int amount) {
        Long chatId = message.getFrom().getId();
        String channelId = message.getForwardFromChat().getId().toString();
        Chat chat;
        try {
            chat = bot.execute(new GetChat(channelId));
            if (chat.getUserName() == null && chat.getInviteLink() == null) {
                return SendMessage.builder()
                        .text("?????? ???? ???? ???????????? ?????????? ????????, ???? ??????????????????????!")
                        .chatId(chatId)
                        .build();
            }
        } catch (TelegramApiException e) {
            return SendMessage.builder()
                    .text("????????????????????????\n\n" +
                            "??????????????????, ???????????????? ???? ?????? ?????? ?????????????????????????????? ???????????? ?????????????")
                    .chatId(chatId)
                    .build();
        }
        float writeOfAmount = amount * channelOrderPrice;

        if (amount >= 5000) {
            amount += 1000;
        } else if (amount >= 2000) {
            amount += 300;
        } else if (amount >= 1000) {
            amount += 100;
        } else if (amount >= 500) {
            amount += 25;
        }

        try {
            bot.execute(channelPromoter.addTaskToInfoChannel(String.valueOf(amount), infoChannelId));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
        User userDB;
        if (optionalUserDB.isPresent()) {
            userDB = optionalUserDB.get();
        } else {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("?????? ???? ?????????? ???? ??????. ???????????????????? ?????????????????????????? ????????")
                    .build();
        }
        Order order = new Order();
        order.setLink(channelId);
        order.setAuthor(userDB);
        order.setAmount(amount);
        order.setType(Order.Type.CHANNEL);
        order.setTasks(new ArrayList<>());

        orderService.add(order);
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    if (advertisingBalance < writeOfAmount) {
                        sendMessageBuilder
                                .text("?????? ???????????????????????? ?????????????? ???? ??????????????!\n" +
                                        "???? ?????????????? " + decimalFormat.format(writeOfAmount - advertisingBalance) + " ???");
                        return;
                    }
                    userService.writeOfFromAdvertising(user.getId(), writeOfAmount);
                    String text = "??? ?????????? ????????????????! ???\n\n" +
                            "\uD83D\uDCB8 ?? ???????????? ?????????????? ?????????????? " + decimalFormat.format(writeOfAmount) + "???\n\n" +
                            "?????? ?? ???????????? ?????????????? ?????????????????????????? ???? ???????????? ???????????? ???? ???????????????? ?????????????????????? ???? ?????????????????? ???????????? ?? ???????????? ??????????????";

                    sendMessageBuilder
                            .text(text);
                },
                () -> sendMessageBuilder.text("?????? ???? ?????????? ???? ??????. ???????????????????? ?????????????????????????? ????????")
        );
        return sendMessageBuilder.build();
    }

    private SendMessage onForwardBotMessage(Update update) {
        Long chatId = update.getMessage().getChatId();
        String botUserName = update.getMessage().getForwardFrom().getUserName();
        Order order = orderService.getOrderByLink(botUserName);
        if (order == null) {
            return bot.error(update);
        }

        User user = new User(chatId);
        Task task = new Task();
        task.setOrder(order);
        task.setUser(user);
        task.setStatus(Task.STATUS.COMPLETED);
        order.setPerformed(order.getPerformed() + 1);
        orderService.add(order);
        taskService.add(task);
        userService.replenishMainBalance(chatId, botStartPrice);

        if (order.getPerformed() >= order.getAmount()) {
            try {
                String link;
                Chat chat;
                try {
                    chat = bot.execute(new GetChat(order.getLink()));
                    link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }

                bot.execute(SendMessage.builder()
                        .chatId(order.getAuthor().getId())
                        .text("????????? ?????????? ???? " + order.getAmount() + " ?????????????????????? ???? ?????????? " + link + " ????????????????!")
                        .disableWebPagePreview(false)
                        .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        return SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDCB0 ?????? ?????????????????? " + decimalFormat.format(botStartPrice) + " ??? ???? ?????????????? ?? ????????!")
                .replyMarkup(keyboards.nextTaskChannelInlineKeyboard())
                .build();
    }
}
