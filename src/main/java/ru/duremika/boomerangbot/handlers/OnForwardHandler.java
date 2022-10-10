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
        switch (lastMessage[0]) {
            case "add_post":
                return promotePosts(update, lastMessage[1]);
            case "add_channel":
                return promoteChannel(message, lastMessage[1]);
            default:
                return bot.error(update);
        }
    }

    private SendMessage promotePosts(Update update, String amount) {
        Message message = update.getMessage();
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");
        String channelName = message.getForwardFromChat().getUserName();
        if (channelName == null) channelName = "c/" + String.valueOf(message.getForwardFromChat().getId()).substring(4);
        String callbackData = channelName + "/" + message.getForwardFromMessageId();

        float writeOfAmount = Integer.parseInt(amount) * postOrderPrice;

        String fromChatId = message.getChatId().toString();
        Integer messageId = message.getMessageId();

        Optional<User> optionalUserDB = userService.findUser(message.getFrom().getId());
        User userDB;
        if (optionalUserDB.isPresent()) {
            userDB = optionalUserDB.get();
        } else {
            return SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("Что то пошло не так. Попробуйте перезапустить бота")
                    .build();
        }

        Order order = new Order();
        order.setLink(callbackData);
        order.setAuthor(userDB);
        order.setAmount(Integer.parseInt(lastMessage[1]));
        order.setType(Order.Type.POST);
        order.setTasks(new ArrayList<>());

        userDB.getOrders().add(order);
        order = orderService.add(order);

        try {
            bot.execute(new ForwardMessage(viewsChannelId, fromChatId, messageId));
            bot.execute(postPromoter.viewPostChecker(viewsChannelId, order.getId()));
            bot.execute(postPromoter.addTaskToInfoChannel(amount, infoChannelId));
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
                                .text("❗️ Недостаточно средств на балансе!\n" +
                                        "Не хватает " + decimalFormat.format(writeOfAmount - advertisingBalance) + " ₽");
                        return;
                    }
                    userService.writeOfFromAdvertising(user.getId(), writeOfAmount);
                    String text = "✅ Пост добавлен! ✅\n\n" +
                            "\uD83D\uDCB8 С вашего баланса списано " + decimalFormat.format(writeOfAmount) + "₽";

                    sendMessageBuilder
                            .text(text);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    private SendMessage promoteChannel(Message message, String amount) {
        Long chatId = message.getFrom().getId();
        String channelId = message.getForwardFromChat().getId().toString();
        Chat chat;
        try {
            chat = bot.execute(new GetChat(channelId));
            if (chat.getUserName() == null && chat.getInviteLink() == null) {
                return SendMessage.builder()
                        .text("⚠️ Вы не выдали права боту, на приглашения!")
                        .chatId(chatId)
                        .build();
            }
        } catch (TelegramApiException e) {
            return SendMessage.builder()
                    .text("❗️Ошибка❗️\n\n" +
                            "Проверьте, является ли наш бот администратором Вашего канала?")
                    .chatId(chatId)
                    .build();
        }
        float writeOfAmount = Integer.parseInt(amount) * channelOrderPrice;

        try {
            bot.execute(channelPromoter.addTaskToInfoChannel(amount, infoChannelId));
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
                    .text("Что то пошло не так. Попробуйте перезапустить бота")
                    .build();
        }
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");

        Order order = new Order();
        order.setLink(channelId);
        order.setAuthor(userDB);
        order.setAmount(Integer.parseInt(lastMessage[1]));
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
                                .text("❗️ Недостаточно средств на балансе!\n" +
                                        "Не хватает " + decimalFormat.format(writeOfAmount - advertisingBalance) + " ₽");
                        return;
                    }
                    userService.writeOfFromAdvertising(user.getId(), writeOfAmount);
                    String text = "✅ Канал добавлен! ✅\n\n" +
                            "\uD83D\uDCB8 С Вашего баланса списано " + decimalFormat.format(writeOfAmount) + "₽\n\n" +
                            "♻️ В случае отписки пользователем от вашего канала вы получите компенсацию на рекламный баланс в полном размере";

                    sendMessageBuilder
                            .text(text);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
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
                        .text("✅Ваш заказ на " + order.getAmount() + " подписчиков на канал " + link + " выполнен!")
                        .disableWebPagePreview(false)
                        .build());
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        return SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDCB0 Вам начислено " + decimalFormat.format(botStartPrice) + " ₽ за переход в бота!")
                .replyMarkup(keyboards.nextTaskChannelInlineKeyboard())
                .build();
    }
}
