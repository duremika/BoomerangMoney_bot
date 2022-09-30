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
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.service.*;

import javax.annotation.PostConstruct;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class OnForwardHandler implements Handler {
    private final TelegramBot bot;
    private final BotConfig config;
    private final UserService userService;
    private final OrderService orderService;
    private final PostPromoter postPromoter;
    private final ChannelPromoter channelPromoter;

    private DecimalFormat decimalFormat;
    private String viewsChannelId;
    private String infoChannelId;

    public OnForwardHandler(@Lazy TelegramBot bot, BotConfig config, UserService userService, OrderService orderService, PostPromoter postPromoter, ChannelPromoter channelPromoter) {
        this.bot = bot;
        this.config = config;
        this.userService = userService;
        this.orderService = orderService;
        this.postPromoter = postPromoter;
        this.channelPromoter = channelPromoter;
    }

    @PostConstruct
    private void init() {
        decimalFormat = config.getDecimalFormat();
        infoChannelId = config.getInfoChannelId();
        viewsChannelId = config.getViewsChannelId();
    }

    @Filter(specialType = "forward")
    public SendMessage forward(Update update) {
        Message message = update.getMessage();
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");

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

        float writeOfAmount = Integer.parseInt(amount) * config.getPostOrderPrice();

        String fromChatId = message.getChatId().toString();
        Integer messageId = message.getMessageId();

        Message messageInViewChannel;
        Message messageInInfoChannel;
        try {
            bot.execute(new ForwardMessage(viewsChannelId, fromChatId, messageId));
            messageInViewChannel = bot.execute(postPromoter.viewPostChecker(message, viewsChannelId));
            messageInInfoChannel = bot.execute(postPromoter.addTaskToInfoChannel(amount, infoChannelId));
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

        Order order = new Order();
        order.setId(callbackData);
        order.setAuthor(userDB);
        order.setAmount(Integer.parseInt(lastMessage[1]));
        order.setType(Order.Type.POST);
        order.setTasks(new ArrayList<>());

        order.setMidInInfoChannel(messageInInfoChannel.getMessageId());
        order.setMidInViewsChannel(messageInViewChannel.getMessageId());

        userDB.getOrders().add(order);
        orderService.add(order);

        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
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
        String channelLink;
        try {
            chat = bot.execute(new GetChat(channelId));
        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
            return SendMessage.builder()
                    .text("❗️Ошибка❗️\n\n" +
                            "Проверьте, является ли наш бот администратором Вашего канала?")
                    .chatId(chatId)
                    .build();
        }
        if (chat.getUserName() != null){
            channelLink = chat.getUserName();
        } else if (chat.getInviteLink() != null) {
            channelLink = chat.getInviteLink().replace("https://t.me/", "");
        } else {
            return SendMessage.builder()
                    .text("❗️Ошибка❗️\n\n" +
                            "Проверьте, является ли наш бот администратором Вашего канала?")
                    .chatId(chatId)
                    .build();
        }
        float writeOfAmount = Integer.parseInt(amount) * config.getChannelOrderPrice();

        Message messageInInfoChannel;
        try {
            messageInInfoChannel= bot.execute(channelPromoter.addTaskToInfoChannel(amount, infoChannelId));
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
        order.setId(channelLink);
        order.setAuthor(userDB);
        order.setAmount(Integer.parseInt(lastMessage[1]));
        order.setType(Order.Type.CHANNEL);
        order.setTasks(new ArrayList<>());

        order.setMidInInfoChannel(messageInInfoChannel.getMessageId());
        orderService.add(order);
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    userService.writeOfFromAdvertising(user.getId(), writeOfAmount);
                    String text = "✅ Канал добавлен! ✅\n\n" +
                            "\uD83D\uDCB8 С Вашего баланса списано " + decimalFormat.format(writeOfAmount) + "₽\n\n" +
                            "♻️ В случае отписки пользователем от Вашего канала Вы получите компенсацию на рекламный баланс в полном размере";

                    sendMessageBuilder
                            .text(text);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }
}
