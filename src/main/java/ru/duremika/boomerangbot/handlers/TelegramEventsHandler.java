package ru.duremika.boomerangbot.handlers;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.keyboards.Keyboards;
import ru.duremika.boomerangbot.service.*;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TelegramEventsHandler implements Handler {
    private final TelegramBot bot;
    private final UserService userService;
    private final OrderService orderService;
    private final TaskService taskService;
    private final BotConfig config;
    private final Keyboards keyboards;

    private DecimalFormat decimalFormat;
    private float postOrderPrice;
    private int minPostOrderAmount;
    private float postViewPrice;


    private float channelOrderPrice;
    private int minChannelOrderAmount;
    private float channelSubscribePrice;


    private float groupOrderPrice;
    private int minGroupOrderAmount;
    private float groupJoinPrice;

    private float botOrderPrice;
    private int minBotOrderAmoun;
    private float botStartPrice;

    private int inviteFriendPrice;

    private float bonusPrice;

    public TelegramEventsHandler(@Lazy TelegramBot bot, UserService userService, OrderService orderService, TaskService taskService, BotConfig config, Keyboards keyboards) {
        this.bot = bot;
        this.userService = userService;
        this.orderService = orderService;
        this.taskService = taskService;
        this.config = config;
        this.keyboards = keyboards;
    }

    @PostConstruct
    private void init() {
        decimalFormat = config.getDecimalFormat();
        postOrderPrice = config.getPostOrderPrice();
        minPostOrderAmount = config.getMinPostOrderAmount();
        postViewPrice = config.getPostViewPrice();


        channelOrderPrice = config.getChannelOrderPrice();
        minChannelOrderAmount = config.getMinChannelOrderAmount();
        channelSubscribePrice = config.getChannelSubscribePrice();

        groupOrderPrice = config.getGroupOrderPrice();
        minGroupOrderAmount = config.getMinGroupOrderAmount();
        groupJoinPrice = config.getGroupJoinPrice();

        botOrderPrice = config.getBotOrderPrice();
        minBotOrderAmoun = config.getMinBotOrderAmount();
        botStartPrice = config.getBotStartPrice();

        inviteFriendPrice = config.getInviteFriendPrice();
        bonusPrice = config.getBonusPrice();


    }

    @Filter(chatMemberUpdated = ChatMemberMember.class, command = "/start")
    SendMessage welcome(Update update) {
        Long chatId = update.hasMyChatMember() ? update.getMyChatMember().getChat().getId() : update.getMessage().getChatId();
        UserService.EnabledStatus status = userService.enableUser(chatId);
        SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder().chatId(chatId);
        switch (status) {
            case NEW_USER:
                messageBuilder
                        .text("✅ Отлично!\nВы зарегистрированы!")
                        .replyMarkup(keyboards.mainReplyKeyboardMarkup());
                return messageBuilder.build();
            case DISABLED_USER:
                messageBuilder
                        .text("✋ С возвращением")
                        .replyMarkup(keyboards.mainReplyKeyboardMarkup());
                return messageBuilder.build();
            case BANNED_USER:
                return messageBuilder.text("Вы заблокированны").build();
            default:
                return null;
        }
    }

    @Filter(chatMemberUpdated = ChatMemberBanned.class)
    void goodbye(Update update) {
        Long chatId = update.getMyChatMember().getChat().getId();
        userService.disableUser(chatId);
    }

    @Filter(text = "\uD83D\uDC68\u200D\uD83D\uDCBB Заработать")
    SendMessage earn(Update update) {
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();

        try {
            if (bot.execute(new GetChatMember(config.getInfoChannelId(), chatId)).getStatus().equals("left")) {
                return notInInfoChannel(chatId);
            } else if (bot.execute(new GetUserProfilePhotos(chatId, 0, 1)).getTotalCount() == 0) {
                return hasNotPhoto(chatId);
            } else if (username == null) {
                return hasNotUsername(chatId);
            } else if (bot.execute(new GetChatMember(config.getViewsChannelId(), chatId)).getStatus().equals("left")) {
                return notInViewerChannel(chatId);
            } else {
                return captcha(chatId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    SendMessage notInInfoChannel(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("❗️ Для использования бота подпишитесь на наш канал: https://t.me/boomerang_money_info")
                .build();
    }

    SendMessage hasNotPhoto(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("Для доступа к данному разделу \n" +
                        "Вам необходимо установить *Фото профиля (аватарку)*\n" +
                        "Инструкция: [Посмотреть!](https://telegra.ph/Kak-postavit-foto-profilya-04-25-2)")
                .parseMode(ParseMode.MARKDOWN)
                .build();
    }

    SendMessage hasNotUsername(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("Для доступа к данному разделу \n" +
                        "Вам необходимо установить *Имя пользователя (@username)*\n" +
                        "Инструкция: [Посмотреть!](https://telegra.ph/Dobavlenie-UserName-04-25)")
                .parseMode(ParseMode.MARKDOWN)
                .build();
    }

    SendMessage notInViewerChannel(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("\uD83D\uDE80 Для заработка подпишитесь на наш канал с просмотрами: https://t.me/boomerang_money_viewer")
                .parseMode(ParseMode.MARKDOWN)
                .build();
    }

    SendMessage captcha(Long id) {
        ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
        int x = threadLocalRandom.nextInt(1, 11);
        int y = threadLocalRandom.nextInt(1, 11);
        int result = x + y;
        return SendMessage.builder()
                .chatId(id)
                .text("Для проверки, что вы не робот, решите пример:\n\n" +
                        x + " + " + y + " =")
                .replyMarkup(keyboards.captchaInlineKeyboard(result))
                .build();
    }

    @Filter(callback = "captcha_fail")
    EditMessageText captchaFail(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("❗️ Вы ошиблись!")
                .build();
    }

    @Filter(callback = {"captcha_success", "earn"})
    EditMessageText earnCaptchaSuccess(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        List<Order> availableOrders = orderService.getAvailableOrders(chatId);

        long availablePostOrders = availableOrders.stream().filter(o -> o.getType().equals(Order.Type.POST)).count();
        long availableChannelOrders = availableOrders.stream().filter(o -> o.getType().equals(Order.Type.CHANNEL)).count();
        long availableGroupOrders = availableOrders.stream().filter(o -> o.getType().equals(Order.Type.GROUP)).count();
        long availableBotOrders = availableOrders.stream().filter(o -> o.getType().equals(Order.Type.BOT)).count();
        long availableExtendedOrders = availableOrders.stream().filter(o -> o.getType().equals(Order.Type.EXTENDED_TASK)).count();

        double allAvailableAmountToEarn =
                availablePostOrders * postViewPrice +
                        availableChannelOrders * channelSubscribePrice +
                        availableGroupOrders * groupJoinPrice +
                        availableBotOrders * botStartPrice;

        String text = "\uD83D\uDCB0 Вы можете заработать: " + decimalFormat.format(allAvailableAmountToEarn) +
                "₽\n\n\uD83D\uDC41 Заданий на просмотр: " + availablePostOrders +
                "\n\uD83D\uDCE2 Заданий на подписку: " + availableChannelOrders +
                "\n\uD83D\uDC64 Заданий на группы: " + availableGroupOrders +
                "\n\uD83E\uDD16 Заданий на боты: " + availableBotOrders +
                "\n♻️ Доп. заработок: " + availableExtendedOrders +
                "\n\n\uD83D\uDD14Выбери способ заработка\uD83D\uDC47";
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .replyMarkup(keyboards.earnInlineKeyboard())
                .build();
    }

    @Filter(callback = {"earn_channel", "next_task_channel"})
    EditMessageText earnSubscribeChannel(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        List<Order> availableOrders = orderService.getAvailableOrders(chatId, Order.Type.CHANNEL);

        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId);

        if (availableOrders.isEmpty()) {
            editMessageTextBuilder
                    .text("\uD83D\uDE1E Задания закончились! Попробуйте позже")
                    .replyMarkup(keyboards.backToEarnInlineKeyboard());
        } else {
            Order order = availableOrders.get(0);
            String link;
            Chat chat;
            try {
                chat = bot.execute(new GetChat(String.valueOf(order.getLink())));
                link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();
                System.out.println(chat);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            editMessageTextBuilder
                    .text("\uD83D\uDCDD Подпишитесь на канал и посмотрите последние 7 постов, затем вернитесь в бот и получите вознаграждение!\n\n" +
                            "⚠️ Запрещено отписываться от каналов, иначе вы можете быть оштрафованы!")
                    .replyMarkup(keyboards.channelEarnInlineKeyboard(link, order.getId()));
        }
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "check_subscribe")
    BotApiMethod<? extends Serializable> checkSubscribe(Update update) {
        String orderId = update.getCallbackQuery().getData().split(" ")[1];
        boolean notSubscribe;
        try {
            Order order = orderService.getOrderById(Long.parseLong(orderId));
            ChatMember chatMember = bot.execute(new GetChatMember(order.getLink(), update.getCallbackQuery().getFrom().getId()));
            notSubscribe = chatMember.getStatus().equals("left");
            System.out.println(chatMember);
        } catch (TelegramApiException e) {
            String callbackQueryId = update.getCallbackQuery().getId();
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("❗️Ошибка ❗️\n\nВы не вступили в канал!")
                    .showAlert(true)
                    .build();
        }
        if (notSubscribe) {
            String callbackQueryId = update.getCallbackQuery().getId();
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("❗️Ошибка ❗️\n\nВы не вступили в канал!")
                    .showAlert(true)
                    .build();
        } else {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String amount = String.valueOf(decimalFormat.format(channelSubscribePrice)).split(",")[1];
            Order order = orderService.getOrderById(Long.parseLong(orderId));

            User user = new User(chatId);
            Task task = new Task();
            task.setOrder(order);
            task.setUser(user);
            task.setStatus(Task.STATUS.COMPLETED);
            order.setPerformed(order.getPerformed() + 1);
            orderService.add(order);
            taskService.add(task);
            userService.replenishMainBalance(chatId, channelSubscribePrice);

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

            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("\uD83D\uDCB0 Вам начислено " + amount + " копеек за подписку на канал!")
                    .replyMarkup(keyboards.nextTaskChannelInlineKeyboard())
                    .build();
        }
    }

    @Filter(callback = "ignore_task")
    EditMessageText ignoreTask(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        String orderId = update.getCallbackQuery().getData().split(" ")[1];
        Order order = orderService.getOrderById(Long.parseLong(orderId));
        Task task = new Task();
        task.setOrder(order);
        task.setUser(new User(chatId));
        task.setStatus(Task.STATUS.IGNORED);
        taskService.add(task);
        return earnSubscribeChannel(update);
    }

    @Filter(callback = {"earn_group", "next_task_group"})
    EditMessageText earnJoinGroup(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        List<Order> availableOrders = orderService.getAvailableOrders(chatId, Order.Type.GROUP);

        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId);

        if (availableOrders.isEmpty()) {
            editMessageTextBuilder
                    .text("\uD83D\uDE1E Задания кончились! Попробуйте позднее")
                    .replyMarkup(keyboards.backToEarnInlineKeyboard());
        } else {
            Order order = availableOrders.get(0);
            String link;
            Chat chat;
            try {
                chat = bot.execute(new GetChat(String.valueOf(order.getLink())));
                link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();
                System.out.println(chat);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
            editMessageTextBuilder
                    .text("\uD83D\uDCDD Вступите в группу, затем вернитесь в бот и получите вознаграждение!\n\n" +
                            "⚠️ Запрещено выходить из групп, иначе Вы можете быть оштрафованы!")
                    .replyMarkup(keyboards.groupEarnInlineKeyboard(link, order.getId()));
        }
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "check_member")
    BotApiMethod<? extends Serializable> checkMember(Update update) {
        String orderId = update.getCallbackQuery().getData().split(" ")[1];
        boolean notSubscribe;
        try {
            Order order = orderService.getOrderById(Long.parseLong(orderId));
            ChatMember chatMember = bot.execute(new GetChatMember(order.getLink(), update.getCallbackQuery().getFrom().getId()));
            notSubscribe = chatMember.getStatus().equals("left");
            System.out.println(chatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            String callbackQueryId = update.getCallbackQuery().getId();
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("❗️Ошибка ❗️\n\nВы не вступили в группу!")
                    .showAlert(true)
                    .build();
        }
        if (notSubscribe) {
            String callbackQueryId = update.getCallbackQuery().getId();
            return AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("❗️Ошибка ❗️\n\nВы не вступили в группу!")
                    .showAlert(true)
                    .build();
        } else {
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            String amount = decimalFormat.format(groupJoinPrice);
            Order order = orderService.getOrderById(Long.parseLong(orderId));

            User user = new User(chatId);
            Task task = new Task();
            task.setOrder(order);
            task.setUser(user);
            task.setStatus(Task.STATUS.COMPLETED);
            order.setPerformed(order.getPerformed() + 1);
            orderService.add(order);
            taskService.add(task);
            userService.replenishMainBalance(chatId, groupJoinPrice);

            if (order.getPerformed() >= order.getAmount()) {
                try {
                    Chat chat = bot.execute(new GetChat(order.getLink()));
                    String link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();

                    bot.execute(SendMessage.builder()
                            .chatId(order.getAuthor().getId())
                            .text("✅Ваш заказ на " + order.getAmount() + " участников в группу " + order.getLink() + " выполнен!")
                            .disableWebPagePreview(false)
                            .build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }

            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text("\uD83D\uDCB0 Вам начислено " + amount + " за вступление в группу!")
                    .replyMarkup(keyboards.nextTaskGroupInlineKeyboard())
                    .build();
        }
    }

    @Filter(text = "\uD83D\uDCE2 Продвижение")
    SendMessage promotion(Update update) {
        Long chatId = update.getMessage().getChatId();
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(chatId);
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    String text = "\uD83D\uDCE2 Что вы хотите продвинуть?\n\n" +
                            "\uD83D\uDCB3 Рекламный баланс: " + decimalFormat.format(user.getBalance().getAdvertising()) + "₽";

                    sendMessageBuilder
                            .text(text)
                            .replyMarkup(keyboards.promotionInlineKeyboard());
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    @Filter(callback = "promotion")
    EditMessageText promotionByCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId);
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    String text = "\uD83D\uDCE2 Что вы хотите продвинуть?\n\n" +
                            "\uD83D\uDCB3 Рекламный баланс: " + decimalFormat.format(user.getBalance().getAdvertising()) + "₽";

                    editMessageTextBuilder
                            .text(text)
                            .replyMarkup(keyboards.promotionInlineKeyboard());
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "post")
    EditMessageText post(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.POST))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.POST))
                .count();
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83D\uDC41 *Наш бот предлагает вам возможность накрутки просмотров на любые посты*\n\n" +
                            " \uD83C\uDF81АКЦИЯ При заказе от:\n" +
                            " *1000* просмотров *+50* в подарок!\n" +
                            " *2000* просмотров *+200* в подарок!\n" +
                            " *5000* просмотров *+750* в подарок!\n" +
                            " *10000* просмотров *+2000* в подарок!\n\n" +
                            "\uD83D\uDC41 1 просмотр - *" + decimalFormat.format(postOrderPrice) + "₽*\n" +
                            "\uD83D\uDCB3 Рекламный баланс - " + decimalFormat.format(advertisingBalance) + "₽\n" +
                            "\uD83D\uDCCA Его хватит на " + (int) (advertisingBalance / postOrderPrice) + " просмотров\n\n" +
                            "⏱ Активных заказов: " + amountActiveOrders +
                            "\n✅ Завершённых заказов: " + amountCompletedOrders;
                    editMessageTextBuilder
                            .text(text)
                            .replyMarkup(keyboards.postInlineKeyboard());
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "add_post")
    EditMessageText addPost(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDD Введите количество просмотров:")
                .build();
    }

    @Filter(callback = "active_post_orders")
    EditMessageText activePostOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDC41 Ваши активные заказы на просмотры:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.POST))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного активного заказа на просмотры");
        } else {
            for (Order order : activeOrderList) {
                text.append("\n▫️ https://t.me/").append(order.getLink())
                        .append(" - Выполнено: ").append(order.getPerformed())
                        .append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "completed_post_orders")
    EditMessageText completedPostOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDC41 Ваши 10 последних, завершённых заказов на просмотры:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.POST))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на просмотры");
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = start; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);
                text.append("\n▫️ https://t.me/").append(order.getLink())
                        .append("\nВыполнено: ").append(order.getPerformed()).append(" из ")
                        .append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    SendMessage amountPosts(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minPostOrderAmount) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minPostOrderAmount + " просмотров!")
                    .replyMarkup(keyboards.toMainReplyKeyboardMarkup());
            return sendMessageBuilder.build();
        }

        Optional<User> optionalUser = userService.findUser(message.getChatId());
        if (optionalUser.isEmpty()) {
            return sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота").build();
        }
        float advertisingBalance = optionalUser.get().getBalance().getAdvertising();
        if (amount * postOrderPrice > advertisingBalance) {
            sendMessageBuilder.text(amount + " просмотров ✖️ " + decimalFormat.format(postOrderPrice) + " копейки = " + decimalFormat.format(amount * postOrderPrice) + " рублей\n\n" +
                    "❗️ Недостаточно средств на балансе! Введите другое число:");
        } else {
            sendMessageBuilder.text(amount + " просмотров ✖️ " + decimalFormat.format(postOrderPrice) + " копейки = " + decimalFormat.format(amount * postOrderPrice) + " рублей\n\n" +
                            "\uD83D\uDCAC *Для запуска задания перешлите пост, который нуждается в продвижении:*")
                    .parseMode(ParseMode.MARKDOWN);
        }
        return sendMessageBuilder.build();
    }

    @Filter(callback = "post_viewed", chatType = Filter.ChatType.CHANNEL)
    AnswerCallbackQuery postViewed(Update update) {
        String callbackQueryId = update.getCallbackQuery().getId();
        String callbackData = update.getCallbackQuery().getData();
        Long orderId = Long.parseLong(callbackData.split(" ")[1]);
        Long chatId = update.getCallbackQuery().getFrom().getId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        AnswerCallbackQuery.AnswerCallbackQueryBuilder answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQueryId)
                .showAlert(true);
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return answer.text("Задание недоступно")
                    .build();
        }
        int availableViews = order.getAmount() - order.getPerformed();
        if (availableViews <= 0) {
            return answer.text("Задание недоступно")
                    .build();
        }
        User user = new User(chatId);
        Task task = taskService.getTaskByOrderId(orderId, user);
        if (task != null) {
            return answer.text("Вы уже просматривали этот пост\n\n" +
                            "\uD83D\uDCB3 Осталось просморов: " + availableViews +
                            "\n\uD83D\uDCB0 Задание из бота: @" + bot.getBotUsername())
                    .build();
        }

        task = new Task();
        task.setOrder(order);
        task.setUser(user);
        task.setStatus(Task.STATUS.COMPLETED);
        order.setPerformed(order.getPerformed() + 1);
        if (order.getPerformed() >= order.getAmount()) {
            try {
                bot.execute(SendMessage.builder()
                        .chatId(order.getAuthor().getId())
                        .text("✅Ваш заказ на " + order.getAmount() + " просмотров поста https://t.me/" + order.getLink() + " выполнен!")
                        .disableWebPagePreview(false)
                        .build());
                bot.execute(DeleteMessage.builder()
                        .chatId(update.getCallbackQuery().getMessage().getChatId())
                        .messageId(update.getCallbackQuery().getMessage().getMessageId())
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        orderService.add(order);
        taskService.add(task);
        userService.replenishMainBalance(chatId, postViewPrice);
        return answer.text("\uD83D\uDC41 За просмотр поста вам начисленно " + decimalFormat.format(postViewPrice) + "₽\n\n" +
                        "\uD83D\uDCB3 Осталось просморов: " + --availableViews +
                        "\n\uD83D\uDCB0 Деньги зачисленны на баланс в боте: @" + bot.getBotUsername())
                .build();
    }

    @Filter(callback = "channel")
    EditMessageText channel(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .count();
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83D\uDCE2 *Наш бот предлагает вам возможность накрутки подписчиков на Ваш ПУБЛИЧНЫЙ и ПРИВАТНЫЙ*\n\n" +
                            " \uD83C\uDF81АКЦИЯ При заказе от:\n" +
                            " *500* подписок *+25* в подарок!\n" +
                            " *1000* подписок *+100* в подарок!\n" +
                            " *2000* просмотров *+300* в подарок!\n" +
                            " *5000* просмотров *+1000* в подарок!\n\n" +
                            "\uD83D\uDC64 1 подписчик - *" + decimalFormat.format(channelOrderPrice) + "₽*\n" +
                            "\uD83D\uDCB3 Рекламный баланс - " + decimalFormat.format(advertisingBalance) + "₽\n" +
                            "\uD83D\uDCCA Его хватит на " + (int) (advertisingBalance / channelOrderPrice) + " подписчиков\n\n" +
                            "⏱ Активных заказов: " + amountActiveOrders +
                            "\n✅ Завершённых заказов: " + amountCompletedOrders +
                            "\n\n❗️ Наш бот @[" + bot.getBotUsername() + "] должен быть администратором продвигаемого канала";
                    editMessageTextBuilder
                            .text(text)
                            .parseMode(ParseMode.MARKDOWN)
                            .replyMarkup(keyboards.channelInlineKeyboard());
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "add_channel")
    EditMessageText addChannel(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDD Введите количество подписчиков:")
                .build();
    }

    SendMessage amountChannelSubscriber(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minChannelOrderAmount) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minChannelOrderAmount + " подписчиков!")
                    .replyMarkup(keyboards.toMainReplyKeyboardMarkup());
            return sendMessageBuilder.build();
        }

        Optional<User> optionalUser = userService.findUser(message.getChatId());
        if (optionalUser.isEmpty()) {
            return sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота").build();
        }
        float advertisingBalance = optionalUser.get().getBalance().getAdvertising();
        if (amount * channelOrderPrice > advertisingBalance) {
            sendMessageBuilder.text(amount + " подписчиков ✖️ " + decimalFormat.format(channelOrderPrice) + " ₽ = " + decimalFormat.format(amount * channelOrderPrice) + " рублей\n\n" +
                    "❗️ Недостаточно средств на балансе! Введите другое число:");
        } else {
            sendMessageBuilder.text(amount + " подписчиков ✖️ " + decimalFormat.format(channelOrderPrice) + " ₽ = " + decimalFormat.format(amount * channelOrderPrice) + " рублей\n\n" +
                    "\uD83D\uDCAC Для запуска задания добавьте нашего бота @" + bot.getBotUsername() + " в администраторы Вашего канала, а затем перешлите любое сообщение из этого канала\n\n" +
                    "⚠️ Канал может быть ПУБЛИЧНЫМ и ПРИВАТНЫМ, не удаляйте бота из админов до конца раскрутки!");
        }
        return sendMessageBuilder.build();
    }

    @Filter(callback = "active_channel_orders")
    EditMessageText activeChannelOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDC41 Ваши активные заказы на подписки:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного активного заказа на подписки");
        } else {
            for (Order order : activeOrderList) {
                Chat chat;
                try {
                    chat = bot.execute(new GetChat(order.getLink()));
                } catch (TelegramApiException e) {
                    continue;
                }
                String link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();
                
                text.append("\n▫️ ").append(link).append(" - Выполнено: ")
                        .append(order.getPerformed()).append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "completed_channel_orders")
    EditMessageText completedChannelOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDC41 Ваши 10 последних, завершённых заказов на подписки:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на подписки");
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = start; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);

                Chat chat;
                try {
                    chat = bot.execute(new GetChat(order.getLink()));
                } catch (TelegramApiException e) {
                    continue;
                }
                String link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();

                text.append("\n▫️ ").append(link)
                        .append("\nВыполнено: ").append(order.getPerformed())
                        .append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "group")
    EditMessageText group(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.GROUP))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.GROUP))
                .count();
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83D\uDC65 *Наш бот предлагает вам уникальную возможность накрутки участников в ПУБЛИЧНЫЕ и ПРИВАТНЫЕ супергруппы*\n\n" +
                            " \uD83C\uDF81АКЦИЯ При заказе от:\n" +
                            " *500* подписок *+25* в подарок!\n" +
                            " *1000* подписок *+100* в подарок!\n" +
                            " *2000* просмотров *+300* в подарок!\n" +
                            " *5000* просмотров *+1000* в подарок!\n\n" +
                            "\uD83D\uDC64 1 участник - *" + decimalFormat.format(groupOrderPrice) + "₽*\n" +
                            "\uD83D\uDCB3 Рекламный баланс - " + decimalFormat.format(advertisingBalance) + "₽\n" +
                            "\uD83D\uDCCA Его хватит на " + (int) (advertisingBalance / groupOrderPrice) + " переходов\n\n" +
                            "⏱ Активных заказов: " + amountActiveOrders +
                            "\n✅ Завершённых заказов: " + amountCompletedOrders;
                    editMessageTextBuilder
                            .text(text)
                            .parseMode(ParseMode.MARKDOWN)
                            .replyMarkup(keyboards.groupInlineKeyboard());
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "add_group")
    EditMessageText addGroup(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDD Введите количество участников:")
                .build();
    }

    SendMessage amountMembersGroup(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minGroupOrderAmount) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minGroupOrderAmount + " участников!")
                    .replyMarkup(keyboards.toMainReplyKeyboardMarkup());
            return sendMessageBuilder.build();
        }

        Optional<User> optionalUser = userService.findUser(message.getChatId());
        if (optionalUser.isEmpty()) {
            return sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота").build();
        }
        float advertisingBalance = optionalUser.get().getBalance().getAdvertising();
        if (amount * groupOrderPrice > advertisingBalance) {
            sendMessageBuilder.text(amount + " участников ✖️ " + decimalFormat.format(groupOrderPrice) + " ₽ = " + decimalFormat.format(amount * groupOrderPrice) + " рублей\n\n" +
                    "❗️ Недостаточно средств на балансе! Введите другое число:");
        } else {
            sendMessageBuilder.text(amount + " участников ✖️ " + decimalFormat.format(groupOrderPrice) + " ₽ = " + decimalFormat.format(amount * groupOrderPrice) + " рублей\n\n" +
                    "\uD83D\uDCAC Для запуска задания добавьте нашего бота @" + bot.getBotUsername() + " в администраторы Вашей группы, а затем отправьте её USERNAME или CHAT_ID вашей группы:");
        }
        return sendMessageBuilder.build();
    }

    @Filter(callback = "active_group_orders")
    EditMessageText activeGroupOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDCE2 Ваши активные заказы на вступления в группы:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.GROUP))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного активного заказа на вступления в группу");
        } else {
            for (Order order : activeOrderList) {
                Chat chat;
                try {
                    chat = bot.execute(new GetChat(order.getLink()));
                } catch (TelegramApiException e) {
                    continue;
                }
                String link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();
                text.append("\n▫️ ").append(link).append(" - Выполнено: ")
                        .append(order.getPerformed()).append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "completed_group_orders")
    EditMessageText completedGroupOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDCE2 Ваши 10 последних, завершённых заказов на вступления в группы:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.GROUP))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на вступления в группу");
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = start; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);

                Chat chat;
                try {
                    chat = bot.execute(new GetChat(order.getLink()));
                } catch (TelegramApiException e) {
                    continue;
                }
                String link = chat.getUserName() != null ? "https://t.me/" + chat.getUserName() : chat.getInviteLink();

                text.append("\n▫️ ").append(link)
                        .append("\nВыполнено: ").append(order.getPerformed())
                        .append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "bot")
    EditMessageText bot(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.BOT))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.BOT))
                .count();
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83E\uDD16 *Наш бот предлагает вам уникальную возможность накрутки переходов на любой бот*\n\n" +
                            " \uD83C\uDF81АКЦИЯ При заказе от:\n" +
                            " *500* переходов *+25* в подарок!\n" +
                            " *1000* переходов *+100* в подарок!\n" +
                            " *2000* переходов *+300* в подарок!\n" +
                            " *5000* переходов *+1000* в подарок!\n\n" +
                            "\uD83D\uDC64 1 переход - *" + decimalFormat.format(botOrderPrice) + "₽*\n" +
                            "\uD83D\uDCB3 Рекламный баланс - " + decimalFormat.format(advertisingBalance) + "₽\n" +
                            "\uD83D\uDCCA Его хватит на " + (int) (advertisingBalance / botOrderPrice) + " переходов\n\n" +
                            "⏱ Активных заказов: " + amountActiveOrders +
                            "\n✅ Завершённых заказов: " + amountCompletedOrders +
                            "\n\n❗️ Возможно продвижение реферальных ссылок";
                    editMessageTextBuilder
                            .text(text)
                            .parseMode(ParseMode.MARKDOWN)
                            .replyMarkup(keyboards.botInlineKeyboard());
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter(callback = "add_bot")
    EditMessageText addBot(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDD Введите количество переходов:")
                .build();
    }

    SendMessage amountStartBot(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minBotOrderAmoun) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minBotOrderAmoun + " участников!")
                    .replyMarkup(keyboards.toMainReplyKeyboardMarkup());
            return sendMessageBuilder.build();
        }

        Optional<User> optionalUser = userService.findUser(message.getChatId());
        if (optionalUser.isEmpty()) {
            return sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота").build();
        }
        float advertisingBalance = optionalUser.get().getBalance().getAdvertising();
        if (amount * botOrderPrice > advertisingBalance) {
            sendMessageBuilder.text(amount + " участников ✖️ " + decimalFormat.format(botOrderPrice) + " ₽ = " + decimalFormat.format(amount * botOrderPrice) + " рублей\n\n" +
                    "❗️ Недостаточно средств на балансе! Введите другое число:");
        } else {
            sendMessageBuilder.text(amount + " участников ✖️ " + decimalFormat.format(botOrderPrice) + " ₽ = " + decimalFormat.format(amount * botOrderPrice) + " рублей\n\n" +
                    "\uD83D\uDCAC Для запуска задания отправьте ссылку на бот (реферальная разрешена), который нуждается в продвижении:");
        }
        return sendMessageBuilder.build();
    }

    @Filter(callback = "active_bot_orders")
    EditMessageText activeBotOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDCE2 Ваши активные заказы на переходы в боты:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.BOT))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного активного заказа на переходы в ботов");
        } else {
            for (Order order : activeOrderList) {
                text.append("\n▫️ https://t.me/").append(order.getLink()).append(" - Выполнено: ")
                        .append(order.getPerformed()).append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(callback = "completed_bot_orders")
    EditMessageText completedBotOrders(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        StringBuilder text = new StringBuilder("\uD83D\uDCE2 Ваши 10 последних, завершённых заказов на переходы в боты:\n");
        List<Order> orderList = orderService.getUserOrders(new User(chatId));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.BOT))
                .sorted(Comparator.comparing(Order::getId))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text.append("\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на переходы в ботов");
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = start; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);
                text.append("\n▫️ https://t.me/").append(order.getLink())
                        .append("\nВыполнено: ").append(order.getPerformed())
                        .append(" из ").append(order.getAmount()).append(" раз");
            }
        }

        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(String.valueOf(text))
                .disableWebPagePreview(true)
                .build();
    }

    @Filter(text = "\uD83D\uDCF1 Мой кабинет")
    SendMessage myOffice(Update update) {
        Long chatId = update.getMessage().getChatId();
        Integer messageId = update.getMessage().getMessageId();
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(chatId);
        userService.findUser(chatId).ifPresentOrElse(
                user -> {
                    long delta = System.currentTimeMillis() - user.getCreatedAt().getTime();
                    long days = TimeUnit.DAYS.convert(delta, TimeUnit.MILLISECONDS);

                    StringBuilder achievmentList = new StringBuilder();
                    if (user.getTasks().size() > 5) achievmentList.append("\uD83D\uDC76");
                    if (user.getTasks().size() > 100) achievmentList.append("🤠");
                    if (user.getTasks().size() > 1000) achievmentList.append("\uD83E\uDEC5");
//                    if (user. sponsor ) achievmentList.append("💸");
                    if (days >= 365) achievmentList.append("⏳");



                    String text = "\uD83D\uDC68\u200D\uD83D\uDCBB Ваш кабинет:" +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n\uD83D\uDD5C Дней в боте: " + days +
                            "\n\uD83D\uDD11 Мой ID: " + user.getId() +
                            "\n\uD83C\uDF10 Мой статус: " + user.getStatus().getTitle() +
                            "\n\uD83C\uDFC6 Мои достижения:⤵\n" +
                            achievmentList +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n✅ Выполнено:" +
                            "\n\uD83D\uDC65 Подписок в каналы: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.CHANNEL && task.getStatus() == Task.STATUS.COMPLETED).count() +
                            "\n\uD83D\uDC65 Подписок в группы: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.GROUP && task.getStatus() == Task.STATUS.COMPLETED).count() +
                            "\n\uD83E\uDD16 Переходов в боты: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.BOT && task.getStatus() == Task.STATUS.COMPLETED).count() +
                            "\n\uD83D\uDC40 Просмотров: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.POST).count() +
                            "\n\uD83D\uDCDD Расширенных заданий: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.EXTENDED_TASK).count() +
                            "\n\uD83C\uDF81 Получено бонусов: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.BONUS).count() +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n\uD83D\uDCB3 Баланс:" +
                            "\n● Основной: " + user.getBalance().getMain() + "₽" +
                            "\n● Рекламный: " + user.getBalance().getAdvertising() + "₽" +
                            "\n● Пополнено: " + user.getBalance().getToppedUp() + "₽" +
                            "\n● Потрачено: " + user.getBalance().getSpent() + "₽" +
                            "\n● Выведено: " + user.getBalance().getOutput() + "₽" +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n\uD83D\uDCB5 Заработано:" +
                            "\n\uD83D\uDCB7 Всего: " + user.getEarned().getTotal() + "₽" +
                            "\n❄️ Заморожено средств: " + user.getEarned().getFrozen() + "₽" +
                            "\n⏳ Ожидается к выплате: " + user.getEarned().getAwait() + "₽";

                    sendMessageBuilder
                            .text(text)
                            .replyMarkup(keyboards.myOfficeInlineKeyboard());
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    @Filter(text = "\uD83D\uDCDA О боте")
    SendMessage aboutBot(Update update) {
        Long chatId = update.getMessage().getChatId();
        return SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(keyboards.aboutBotInlineKeyboard())
                .build();
    }

    @Filter(callback = "about_bot")
    EditMessageText aboutBotByCallback(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(keyboards.aboutBotInlineKeyboard())
                .build();
    }

    @Filter(callback = "chat")
    EditMessageText chat(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("❤️ Чтобы перейти в чат, нажмите ссылку ниже:")
                .replyMarkup(keyboards.chatInlineKeyboard())
                .build();
    }

    @Filter(callback = "rules")
    EditMessageText rules(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️Используя данный бот, вы автоматически соглашаетесь с " +
                        "правилами которые описаны ниже по ссылке, любые ваши " +
                        "операции и действия на проекте расцениваются " +
                        "администрацией как ваше согласие на их проведение!\n\n" +
                        "♻️Правила бота: [читать!](https://telegra.ph/Pravila-bota-TGSTAR-BOT-12-14)")
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .replyMarkup(keyboards.rulesInlineKeyboard())
                .build();
    }

    @Filter(callback = "admin")
    EditMessageText administration(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️ Обращаться к администратору в случае:\n\n" +
                        "1. Если обнаружен баг (ошибка).\n" +
                        "2. У вас есть деловое предложение.\n" +
                        "3. Хотите иметь собственного бота.\n" +
                        "4. Запрещено спрашивать по поводу выплат.")
                .replyMarkup(keyboards.administrationInlineKeyboard())
                .build();
    }

    @Filter(callback = "want_bot")
    EditMessageText wantBot(Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️ Если вы хотите заказать бот, напишите разработчикам, кнопка ниже:")
                .replyMarkup(keyboards.wantBotInlineKeyboard())
                .build();
    }

    @Filter(specialType = "number")
    SendMessage number(Update update) {
        Message message = update.getMessage();
        Long chatId = message.getChatId();
        String lastMessage = userService.getLastMessage(chatId);
        if (lastMessage == null) {
            return bot.error(update);
        }
        switch (lastMessage) {
            case "add_post":
                return amountPosts(message);
            case "add_channel":
                return amountChannelSubscriber(message);
            case "add_group":
                return amountMembersGroup(message);
            case "add_bot":
                return amountStartBot(message);
            default:
                return bot.error(update);
        }
    }

    @Filter(text = "◀️ На главную")
    SendMessage toMain(Update update) {
        Long chatId = update.getMessage().getChatId();
        return SendMessage.builder()
                .text("Вы в главном меню")
                .chatId(chatId)
                .replyMarkup(keyboards.mainReplyKeyboardMarkup())
                .build();
    }
}
