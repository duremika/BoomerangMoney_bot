package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.duremika.boomerangbot.constants.Keyboards;
import ru.duremika.boomerangbot.exception.UserBannedException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class TelegramEventsHandler {
    private final UserService userService;

    public TelegramEventsHandler(UserService userService) {
        this.userService = userService;
    }


    SendMessage welcome(Long id) {
        boolean isNewUser;
        try {
            isNewUser = userService.createOrUpdateUser(id);
        } catch (UserBannedException e) {
            return SendMessage.builder()
                    .chatId(id)
                    .text("Вы заблокированны")
                    .build();
        }
        String text = isNewUser ? "✅ Отлично!\nВы зарегистрированы!" : "✋ С возвращением";
        return SendMessage.builder()
                .chatId(id)
                .text(text)
                .replyMarkup(Keyboards.mainReplyKeyboardMarkup)
                .build();
    }

    void goodbye(Long id) {
        userService.disableUser(id);
    }


    SendMessage promotion(Long id) {
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(id);
        userService.findUser(id).ifPresentOrElse(
                user -> {
                    String text = "\uD83D\uDCE2 Что вы хотите продвинуть?\n\n" +
                            "\uD83D\uDCB3 Рекламный баланс: " + user.getBalance().getAdvertising() + "₽";

                    sendMessageBuilder
                            .text(text)
                            .replyMarkup(Keyboards.promotionInlineKeyboard);
                },
                () -> sendMessageBuilder.text("Что то пошло не так. Попробуйте перезапустить бота")
        );
        return sendMessageBuilder.build();
    }

    SendMessage notInInfoChannel(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("❗️ Для использования бота подпишитесь на наш канал: [https://t.me/boomerang_money_info](t.me/boomerang_money_info)")
                .parseMode("Markdown")
                .build();
    }

    SendMessage hasNotPhoto(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("Для доступа к данному разделу \n" +
                        "Вам необходимо установить **Фото профиля (аватарку)**\n" +
                        "Инструкция: [Посмотреть!](https://telegra.ph/Kak-postavit-foto-profilya-04-25-2)")
                .parseMode("Markdown")
                .build();
    }


    SendMessage hasNotUsername(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("Для доступа к данному разделу \n" +
                        "Вам необходимо установить **Имя пользователя (@username)**\n" +
                        "Инструкция: [Посмотреть!](https://telegra.ph/Dobavlenie-UserName-04-25)")
                .parseMode("Markdown")
                .build();
    }


    SendMessage notInViewerChannel(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("\uD83D\uDE80 Для заработка подпишитесь на наш канал с просмотрами: [https://t.me/boomerang_money_viewer](t.me/boomerang_money_viewer)")
                .parseMode("Markdown")
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
                       x + " + " + y + " =" )
                .replyMarkup(Keyboards.captchaInlineKeyboard(result))
                .build();
    }

    EditMessageText captchaFail(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("❗️ Вы ошиблись!" )
                .build();
    }

    SendMessage myOffice(Long id) {
        SendMessage.SendMessageBuilder sendMessageBuilder = SendMessage.builder()
                .chatId(id);
        userService.findUser(id).ifPresentOrElse(
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
                            "\n\uD83D\uDC65 Подписок в каналы: " + user.getTasks().getChannels() +
                            "\n\uD83D\uDC65 Подписок в группы: " + user.getTasks().getGroups() +
                            "\n\uD83E\uDD16 Переходов в боты: " + user.getTasks().getBots() +
                            "\n\uD83D\uDC40 Просмотров: " + user.getTasks().getViews() +
                            "\n\uD83D\uDCDD Расширенных заданий: " + user.getTasks().getExtendedTask() +
                            "\n\uD83C\uDF81 Получено бонусов: " + user.getTasks().getBonuses() +
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

    SendMessage aboutBot(Long id) {
        return SendMessage.builder()
                .chatId(id)
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(Keyboards.aboutBotInlineKeyboard)
                .build();
    }

    EditMessageText aboutBot(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("\uD83D\uDCDA Информация о нашем боте:")
                .replyMarkup(Keyboards.aboutBotInlineKeyboard)
                .build();
    }

    EditMessageText chat(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("❤️ Чтобы перейти в чат, нажмите ссылку ниже:")
                .replyMarkup(Keyboards.chatInlineKeyboard)
                .build();
    }

    EditMessageText rules(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️Используя данный бот, вы автоматически соглашаетесь с " +
                        "правилами которые описаны ниже по ссылке, любые ваши " +
                        "операции и действия на проекте расцениваются " +
                        "администрацией как ваше согласие на их проведение!\n\n" +
                        "♻️Правила бота: [читать!](https://telegra.ph/Pravila-bota-TGSTAR-BOT-12-14)")
                .parseMode("Markdown")
                .disableWebPagePreview(true)
                .replyMarkup(Keyboards.rulesInlineKeyboard)
                .build();
    }


    EditMessageText administration(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️ Обращаться к администратору в случае:\n\n" +
                        "1. Если обнаружен баг (ошибка).\n" +
                        "2. У вас есть деловое предложение.\n" +
                        "3. Хотите иметь собственного бота.\n" +
                        "4. Запрещено спрашивать по поводу выплат.")
                .replyMarkup(Keyboards.administrationInlineKeyboard)
                .build();
    }

    EditMessageText wantBot(Long chatId, Integer messageId) {
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("⚠️ Если вы хотите заказать бот, напишите разработчикам, кнопка ниже:")
                .replyMarkup(Keyboards.wantBotInlineKeyboard)
                .build();
    }


}
