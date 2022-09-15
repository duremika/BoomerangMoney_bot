package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.duremika.boomerangbot.config.BotConfig;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    final TelegramEventsHandler eventsHandler;
    final String infoChannelId = "-1001697520335";
    final String viewerChannelId = "-1001718302900";

    public TelegramBot(BotConfig config, TelegramEventsHandler eventsHandler) {
        this.config = config;

        this.eventsHandler = eventsHandler;
    }

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
        log.info(update.toString());
        if ((update.hasMessage() && update.getMessage().getChat().getType().equals("private")) ||
                (update.hasCallbackQuery() && update.getCallbackQuery().getMessage().getChat().getType().equals("private"))) {
            if ((update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("member")) ||
                    update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().equals("/start")) {
                try {
                    Long id = update.getMyChatMember().getChat().getId();
                    execute(eventsHandler.welcome(id));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasMyChatMember() && update.getMyChatMember().getNewChatMember().getStatus().equals("kicked")) {
                Long id = update.getMyChatMember().getChat().getId();
                eventsHandler.goodbye(id);
            } else if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().contains("\uD83D\uDCE2")) {
                Long id = update.getMessage().getChatId();
                try {
                    execute(eventsHandler.promotion(id));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().contains("\uD83D\uDC68\u200D\uD83D\uDCBB")) {
                Long uid = update.getMessage().getFrom().getId();
                Long chatId = update.getMessage().getChatId();
                try {
                    if (execute(new GetChatMember(infoChannelId, uid)).getStatus().equals("left")) {
                        execute(eventsHandler.notInInfoChannel(chatId));
                    } else if (execute(new GetUserProfilePhotos(uid, 0, 1)).getTotalCount() == 0) {
                        execute(eventsHandler.hasNotPhoto(chatId));
                    } else if (update.getMessage().getFrom().getUserName() == null) {
                        execute(eventsHandler.hasNotUsername(chatId));
                    } else if (execute(new GetChatMember(viewerChannelId, uid)).getStatus().equals("left")) {
                        execute(eventsHandler.notInViewerChannel(chatId));
                    } else {
                        execute(eventsHandler.captcha(chatId));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("captcha fail")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.captchaFail(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("captcha success")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    log.info("success");
                    execute(eventsHandler.captchaFail(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().contains("\uD83D\uDCF1")) {
                Long id = update.getMessage().getChatId();
                try {
                    execute(eventsHandler.myOffice(id));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().contains("\uD83D\uDCDA")) {
                Long id = update.getMessage().getChatId();
                try {
                    execute(eventsHandler.aboutBot(id));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("about bot")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.aboutBot(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("chat")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.chat(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("rules")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.rules(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("admin")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.administration(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery().getData().equals("want bot")) {
                Long chatId = update.getCallbackQuery().getMessage().getChatId();
                Integer mId = update.getCallbackQuery().getMessage().getMessageId();
                try {
                    execute(eventsHandler.wantBot(chatId, mId));
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
