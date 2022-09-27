package ru.duremika.boomerangbot.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.annotations.ChatType;
import ru.duremika.boomerangbot.annotations.Filter;
import ru.duremika.boomerangbot.annotations.Handler;
import ru.duremika.boomerangbot.config.BotConfig;
import ru.duremika.boomerangbot.constants.Keyboards;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TelegramEventsHandler implements Handler {
    private final UserService userService;
    private final OrderService orderService;
    private final TaskService taskService;
    private final TelegramBot bot;
    private final BotConfig config;

    final DecimalFormat decimalFormat = new DecimalFormat("#.##");

    public final static float postOrderPrice = 0.05f;
    public final static int minPostOrderAmount = 50;
    public final static float postViewPrice = 0.03f;


    public final static float channelOrderPrice = 0.25f;
    public final static int minChannelOrderAmount = 2;
    public final static float channelSubscribePrice = 0.2f;

    public final static float groupJoinPrice = 0.3f;

    public final static float botStartPrice = 0.15f;

    public final static int inviteFriend = 1;

    public final static float bonusPrice = 0.021f;

    public TelegramEventsHandler(UserService userService, OrderService orderService, TaskService taskService, @Lazy TelegramBot bot, BotConfig config) {
        this.userService = userService;
        this.orderService = orderService;
        this.taskService = taskService;
        this.bot = bot;
        this.config = config;
    }

    @Filter({"member", "/start"})
    SendMessage welcome(Message message) {
        EnabledStatus status = userService.enableUser(message.getChatId());
        SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder().chatId(message.getChatId());
        switch (status) {
            case NEW_USER:
                messageBuilder
                        .text("✅ Отлично!\nВы зарегистрированы!")
                        .replyMarkup(Keyboards.mainReplyKeyboardMarkup);
                return messageBuilder.build();
            case DISABLED_USER:
                messageBuilder
                        .text("✋ С возвращением")
                        .replyMarkup(Keyboards.mainReplyKeyboardMarkup);
                return messageBuilder.build();
            case BANNED_USER:
                return messageBuilder.text("Вы заблокированны").build();
            default:
                return null;
        }
    }

    @Filter("kicked")
    void goodbye(Message message) {
        userService.disableUser(message.getChatId());
    }

    @Filter("\uD83D\uDC68\u200D\uD83D\uDCBB Заработать")
    SendMessage earn(Message message) {
        Long uid = message.getFrom().getId();
        Long chatId = message.getChatId();

        try {
            if (bot.checkSubscribeToInfoChannel(uid)) {
                return notInInfoChannel(chatId);
            } else if (bot.checkProfilePhoto(uid)) {
                return hasNotPhoto(chatId);
            } else if (message.getFrom().getUserName() == null) {
                return hasNotUsername(chatId);
            } else if (bot.checkSubscribeToViewsChannel(uid)) {
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
                .text("❗️ Для использования бота подпишитесь на наш канал: [https://t.me/boomerang_money_info](t.me/boomerang_money_info)")
                .parseMode(ParseMode.MARKDOWN)
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
                .text("\uD83D\uDE80 Для заработка подпишитесь на наш канал с просмотрами: [https://t.me/boomerang_money_viewer](t.me/boomerang_money_viewer)")
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
                .replyMarkup(Keyboards.captchaInlineKeyboard(result))
                .build();
    }

    @Filter("captcha_fail")
    EditMessageText captchaFail(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("❗️ Вы ошиблись!")
                .build();
    }

    @Filter("captcha_success")
    EditMessageText earnCaptchaSuccess(Message message) {
        Long userId = message.getChatId();
        List<Order> availableOrders = orderService.getAvailableOrders(userId);

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
                "\n\uD83E\uDD16 Заданий на боты: " + availableGroupOrders +
                "\n♻️ Доп. заработок: " + availableExtendedOrders +
                "\n\n\uD83D\uDD14Выбери способ заработка\uD83D\uDC47";
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .replyMarkup(Keyboards.earnInlineKeyboard)
                .build();
    }

    @Filter("\uD83D\uDCE2 Продвижение")
    SendMessage promotion(Message message) {
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    String text = "\uD83D\uDCE2 Что вы хотите продвинуть?\n\n" +
                            "\uD83D\uDCB3 Рекламный баланс: " + decimalFormat.format(user.getBalance().getAdvertising()) + "₽";

                    sendMessageBuilder
                            .text(text)
                            .replyMarkup(Keyboards.promotionInlineKeyboard);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    @Filter("promotion")
    EditMessageText promotionByCallback(Message message) {
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    String text = "\uD83D\uDCE2 Что вы хотите продвинуть?\n\n" +
                            "\uD83D\uDCB3 Рекламный баланс: " + decimalFormat.format(user.getBalance().getAdvertising()) + "₽";

                    editMessageTextBuilder
                            .text(text)
                            .replyMarkup(Keyboards.promotionInlineKeyboard);
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter("post")
    EditMessageText post(Message message) {
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.POST))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.POST))
                .count();
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83D\uDC41 *Наш бот предлагает Вам возможность накрутки просмотров на любые посты*\n\n" +
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
                            .replyMarkup(Keyboards.postInlineKeyboard);
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter("add_post")
    EditMessageText addPost(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("\uD83D\uDCDD Введите количество просмотров:")
                .build();
    }

    @Filter("active_post_orders")
    EditMessageText activePostOrders(Message message) {
        String text = "\uD83D\uDC41 Ваши активные заказы на просмотры:\n";
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.POST))
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text += "\n\uD83D\uDE1E У Вас нет ни одного активного заказа на просмотры";
        } else {
            for (Order order : activeOrderList) {
                text += "\n▫️ [https://t.me/" + order.getId() + "](https://t.me/" + order.getId() + ") - " +
                        "Выполнено: " + order.getPerformed() + " из " + order.getAmount() + " раз";
            }
        }

        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build();
    }

    @Filter("completed_post_orders")
    EditMessageText completedPostOrders(Message message) {
        String text = "\uD83D\uDC41 Ваши 10 последних, завершённых заказов на просмотры:\n";
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.POST))
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text += "\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на просмотры";
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = 0; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);
                text += "\n▫️ [https://t.me/" + order.getId() + "](https://t.me/" + order.getId() + ")\n" +
                        "Выполнено: " + order.getPerformed() + " из " + order.getAmount() + " раз";
            }
        }

        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build();
    }

    SendMessage amountPosts(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minPostOrderAmount) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minPostOrderAmount + " просмотров")
                    .replyMarkup(Keyboards.toMainInlineKeyboard);
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

    SendMessage promotePosts(Message message, String amount) {
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");
        String callbackData = message.getForwardFromChat().getUserName() + "/" + message.getMessageId();


        float writeOfAmount = Integer.parseInt(amount) * postOrderPrice;

        String viewsChannelId = config.getViewsChannelId();
        String infoChannelId = config.getInfoChannelId();

        String fromChatId = message.getChatId().toString();
        Integer messageId = message.getMessageId();


        Message messageInViewChannel;
        Message messageInInfoChannel;
        try {
            bot.execute(new ForwardMessage(viewsChannelId, fromChatId, messageId));
            messageInViewChannel = bot.execute(PostPromoter.viewPostChecker(message, viewsChannelId));
            messageInInfoChannel = bot.execute(PostPromoter.addTaskToInfoChannel(message, amount, infoChannelId));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        User userDB = userService.findUser(message.getFrom().getId()).get();
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

    @Filter(value = "post_viewed", chatType = ChatType.CHANNEL)
    AnswerCallbackQuery postViewed(Message message) {
        AnswerCallbackQuery.AnswerCallbackQueryBuilder answer = AnswerCallbackQuery.builder()
                .callbackQueryId(message.getCaption())
                .showAlert(true);
        Order order = orderService.getOrderById(message.getText());
        if (order == null) {
            return answer.text("Задание недоступно")
                    .build();
        }
        int availableViews = order.getAmount() - order.getPerformed();
        if (availableViews <= 0) {
            return answer.text("Задание недоступно")
                    .build();
        }
        User user = new User(message.getFrom().getId());
        Task task = taskService.getTaskByOrderId(message.getText(), user);
        if (task != null) {
            return answer.text("Вы уже просматривали этот пост\n\n" +
                            "\uD83D\uDCB3 Осталось просморов: " + availableViews +
                            "\n\uD83D\uDCB0 Задание из бота: @" + bot.getBotUsername())
                    .build();
        }

        task = new Task();
        task.setOrder(order);
        task.setUser(user);
        order.setPerformed(order.getPerformed() + 1);
        if (order.getPerformed() >= order.getAmount()) {
            try {
                bot.execute(SendMessage.builder()
                        .chatId(order.getAuthor().getId())
                        .text("✅Ваш заказ на " + order.getAmount() + " просмотров поста [https://t.me/" + order.getId() + "](https://t.me/" + order.getId() + ") выполнен!")
                        .parseMode(ParseMode.MARKDOWN)
                        .disableWebPagePreview(false)
                        .build());
                bot.execute(DeleteMessage.builder()
                        .chatId(config.getViewsChannelId())
                        .messageId(order.getMidInViewsChannel())
                        .build());
                bot.execute(DeleteMessage.builder()
                        .chatId(config.getInfoChannelId())
                        .messageId(order.getMidInInfoChannel())
                        .build());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        orderService.add(order);
        taskService.add(task);
        userService.replenishMainBalance(message.getFrom().getId(), postViewPrice);
        return answer.text("\uD83D\uDC41 За просмотр поста вам начисленно " + decimalFormat.format(postViewPrice) + "₽\n\n" +
                        "\uD83D\uDCB3 Осталось просморов: " + --availableViews +
                        "\n\uD83D\uDCB0 Деньги зачисленны на баланс в боте: @" + bot.getBotUsername())
                .build();
    }

    @Filter("channel")
    EditMessageText channel(Message message) {
        EditMessageText.EditMessageTextBuilder editMessageTextBuilder = EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .parseMode(ParseMode.MARKDOWN);
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        long amountActiveOrders = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .count();
        long amountCompletedOrders = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .count();
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    float advertisingBalance = user.getBalance().getAdvertising();
                    String text = "\uD83D\uDCE2 *Наш бот предлагает Вам возможность накрутки подписчиков на Ваш ПУБЛИЧНЫЙ и ПРИВАТНЫЙ*\n\n" +
                            " \uD83C\uDF81АКЦИЯ При заказе от:\n" +
                            " *500* подписок *+25* в подарок!\n" +
                            " *1000* подписок *+100* в подарок!\n" +
                            " *2000* просмотров *+300* в подарок!\n" +
                            " *5000* просмотров *+1000* в подарок!\n\n" +
                            "\uD83D\uDC64 1 подписчик - *" + decimalFormat.format(channelOrderPrice) + "₽*\n" +
                            "\uD83D\uDCB3 Рекламный баланс - " + decimalFormat.format(advertisingBalance) + "₽\n" +
                            "\uD83D\uDCCA Его хватит на " + (int) (advertisingBalance / channelOrderPrice) + " просмотров\n\n" +
                            "⏱ Активных заказов: " + amountActiveOrders +
                            "\n✅ Завершённых заказов: " + amountCompletedOrders +
                            "\n\n❗️ Наш бот @[" + bot.getBotUsername() + "] должен быть администратором продвигаемого канала";
                    editMessageTextBuilder
                            .text(text)
                            .parseMode(ParseMode.MARKDOWN)
                            .replyMarkup(Keyboards.channelInlineKeyboard);
                },
                () -> editMessageTextBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return editMessageTextBuilder.build();
    }

    @Filter("add_channel")
    EditMessageText addChannel(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("\uD83D\uDCDD Введите количество подписчиков:")
                .build();
    }

    SendMessage amountChannelSubscriber(Message message) {
        int amount = Integer.parseInt(message.getText());
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        if (amount < minChannelOrderAmount) {
            sendMessageBuilder.text("❗️Ошибка❗️\n\n" +
                            "Минимальный заказ - " + minChannelOrderAmount + " подписчиков")
                    .replyMarkup(Keyboards.toMainInlineKeyboard);
            return sendMessageBuilder.build();
        }

        Optional<User> optionalUser = userService.findUser(message.getChatId());
        if (optionalUser.isEmpty()) {
            return sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота").build();
        }
        float advertisingBalance = optionalUser.get().getBalance().getAdvertising();
        if (amount * channelOrderPrice > advertisingBalance) {
            sendMessageBuilder.text(amount + " просмотров ✖️ " + decimalFormat.format(channelOrderPrice) + " ₽ = " + decimalFormat.format(amount * channelOrderPrice) + " рублей\n\n" +
                    "❗️ Недостаточно средств на балансе! Введите другое число:");
        } else {
            sendMessageBuilder.text(amount + " просмотров ✖️ " + decimalFormat.format(channelOrderPrice) + " ₽ = " + decimalFormat.format(amount * channelOrderPrice) + " рублей\n\n" +
                    "\uD83D\uDCAC Для запуска задания добавьте нашего бота @" + bot.getBotUsername() + " в администраторы Вашего канала, а затем перешлите любое сообщение из этого канала\n\n" +
                    "⚠️ Канал может быть ПУБЛИЧНЫМ и ПРИВАТНЫМ, не удаляйте бота из админов до конца раскрутки!");
        }
        return sendMessageBuilder.build();
    }

    SendMessage promoteChannel(Message message, String amount) {
        Long chatId = message.getFrom().getId();
        String channelId = message.getForwardFromChat().getId().toString();
        try {
            var me = bot.execute(new GetMe());
            boolean botIsAdmin = bot.execute(new GetChatAdministrators(channelId)).stream()
                    .anyMatch(chatMember -> chatMember.getUser().equals(me));
            Chat chat = bot.execute(new GetChat(channelId));
            if (botIsAdmin && chat.getInviteLink() != null) {
                return SendMessage.builder()
                        .text("❗️Ошибка❗️\n\n" +
                                "Проверьте, является ли наш бот администратором Вашего канала?")
                        .chatId(chatId)
                        .build();
            }

        } catch (TelegramApiException e) {
            System.out.println(e.getMessage());
            return SendMessage.builder()
                    .text("❗️Ошибка❗️\n\n" +
                            "Проверьте, является ли наш бот администратором Вашего канала?")
                    .chatId(chatId)
                    .build();
        }
        float writeOfAmount = Integer.parseInt(amount) * channelOrderPrice;
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

    @Filter("active_channel_orders")
    EditMessageText activeChannelOrders(Message message) {
        String text = "\uD83D\uDC41 Ваши активные заказы на подписки:\n";
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        List<Order> activeOrderList = orderList.stream()
                .filter(order -> order.getAmount() > order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());


        if (activeOrderList.size() == 0) {
            text += "\n\uD83D\uDE1E У Вас нет ни одного активного заказа на подписки";
        } else {
            for (Order order : activeOrderList) {
                text += "\n▫️ [https://t.me/" + order.getId() + "](https://t.me/" + order.getId() + ") - " +
                        "Выполнено: " + order.getPerformed() + " из " + order.getAmount() + " раз";
            }
        }

        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build();
    }

    @Filter("completed_channel_orders")
    EditMessageText completedChannelOrders(Message message) {
        String text = "\uD83D\uDC41 Ваши 10 последних, завершённых заказов на подписки:\n";
        List<Order> orderList = orderService.getUserOrders(new User(message.getChatId()));
        List<Order> completedOrderList = orderList.stream()
                .filter(order -> order.getAmount() <= order.getPerformed() && order.getType().equals(Order.Type.CHANNEL))
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());

        if (completedOrderList.size() == 0) {
            text += "\n\uD83D\uDE1E У Вас нет ни одного завершённого заказа на подписки";
        } else {
            int start = completedOrderList.size() <= 10 ? 0 : completedOrderList.size() - 11;
            for (int i = 0; i < completedOrderList.size(); i++) {
                Order order = completedOrderList.get(i);
                text += "\n▫️ [https://t.me/" + order.getId() + "](https://t.me/" + order.getId() + ")\n" +
                        "Выполнено: " + order.getPerformed() + " из " + order.getAmount() + " раз";
            }
        }

        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text(text)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .build();
    }

    @Filter("\uD83D\uDCF1 Мой кабинет")
    SendMessage myOffice(Message message) {
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(message.getChatId());
        userService.findUser(message.getChatId()).ifPresentOrElse(
                user -> {
                    long delta = System.currentTimeMillis() - user.getCreatedAt().getTime();
                    long days = TimeUnit.DAYS.convert(delta, TimeUnit.MILLISECONDS);
                    String text = "\uD83D\uDC68\u200D\uD83D\uDCBB Ваш кабинет:" +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n\uD83D\uDD5C Дней в боте: " + days +
                            "\n\uD83D\uDD11 Мой ID: " + user.getId() +
                            "\n\uD83C\uDF10 Мой статус: " + user.getStatus().getTitle() +
                            "\n\uD83C\uDFC6 Мои достижения:⤵" +
                            "\n" +
                            "\n➖➖➖➖➖➖➖➖➖" +
                            "\n✅ Выполнено:" +
                            "\n\uD83D\uDC65 Подписок в каналы: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.CHANNEL).count() +
                            "\n\uD83D\uDC65 Подписок в группы: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.GROUP).count() +
                            "\n\uD83E\uDD16 Переходов в боты: " + user.getTasks().stream().filter(task -> task.getOrder().getType() == Order.Type.BOT).count() +
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
                            .replyMarkup(Keyboards.myOfficeInlineKeyboard);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    @Filter("\uD83D\uDCDA О боте")
    SendMessage aboutBot(Message message) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(Keyboards.aboutBotInlineKeyboard)
                .build();
    }

    @Filter("about_bot")
    EditMessageText aboutBotByCallback(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(Keyboards.aboutBotInlineKeyboard)
                .build();
    }

    @Filter("chat")
    EditMessageText chat(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("❤️ Чтобы перейти в чат, нажмите ссылку ниже:")
                .replyMarkup(Keyboards.chatInlineKeyboard)
                .build();
    }

    @Filter("rules")
    EditMessageText rules(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("⚠️Используя данный бот, вы автоматически соглашаетесь с " +
                        "правилами которые описаны ниже по ссылке, любые ваши " +
                        "операции и действия на проекте расцениваются " +
                        "администрацией как ваше согласие на их проведение!\n\n" +
                        "♻️Правила бота: [читать!](https://telegra.ph/Pravila-bota-TGSTAR-BOT-12-14)")
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .replyMarkup(Keyboards.rulesInlineKeyboard)
                .build();
    }

    @Filter("admin")
    EditMessageText administration(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("⚠️ Обращаться к администратору в случае:\n\n" +
                        "1. Если обнаружен баг (ошибка).\n" +
                        "2. У вас есть деловое предложение.\n" +
                        "3. Хотите иметь собственного бота.\n" +
                        "4. Запрещено спрашивать по поводу выплат.")
                .replyMarkup(Keyboards.administrationInlineKeyboard)
                .build();
    }

    @Filter("want_bot")
    EditMessageText wantBot(Message message) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .text("⚠️ Если вы хотите заказать бот, напишите разработчикам, кнопка ниже:")
                .replyMarkup(Keyboards.wantBotInlineKeyboard)
                .build();
    }

    @Filter("error")
    SendMessage error(Message message) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text("\uD83E\uDD28")
                .build();
    }

    @Filter("number")
    SendMessage number(Message message) {
        String lastMessage = userService.getLastMessage(message.getChatId());
        if (lastMessage == null) {
            return error(message);
        }
        switch (lastMessage) {
            case "add_post":
                return amountPosts(message);
            case "add_channel":
                return amountChannelSubscriber(message);
            default:
                return error(message);
        }
    }

    @Filter("forward")
    SendMessage forward(Message message) {
        String[] lastMessage = userService.getLastMessage(message.getChatId()).split(" ");

        if (lastMessage.length != 2 || lastMessage[0] == null || lastMessage[1] == null) {
            return error(message);
        }
        switch (lastMessage[0]) {
            case "add_post":
                return promotePosts(message, lastMessage[1]);
            case "add_channel":
                return promoteChannel(message, lastMessage[1]);
            default:
                return error(message);
        }
    }


    @Filter("◀️ На главную")
    SendMessage toMain(Message message) {
        return SendMessage.builder()
                .text("Вы в главном меню")
                .chatId(message.getChatId())
                .replyMarkup(Keyboards.mainReplyKeyboardMarkup)
                .build();
    }
}
