package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.duremika.boomerangbot.constants.Keyboards;
import ru.duremika.boomerangbot.exception.UserBannedException;

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
}
