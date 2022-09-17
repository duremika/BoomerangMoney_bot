package ru.duremika.boomerangbot.service;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;

@SpringBootTest
@RunWith(SpringRunner.class)
class TelegramEventsHandlerTest {
    private final Update updateWithMessage = new Update() {{
        setMessage(new Message() {{
            setChat(new Chat() {{
                setId(123L);
                setType("private");
            }});
        }});
    }};

    @MockBean
    UserService userService;
    @Autowired
    TelegramBot telegramBot;

    TelegramBot spyBot;
    TelegramEventsHandler spyEventsHandler;

    @PostConstruct
    public void setUp(){
        spyBot = Mockito.spy(telegramBot);
        spyEventsHandler = Mockito.spy(spyBot.eventsHandler);
        spyBot.eventsHandler = spyEventsHandler;
        try {
            Mockito.doReturn(null).when(spyBot).execute(Mockito.any(BotApiMethod.class));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void welcome_member() {
        updateWithMessage.getMessage().setText("member");
        Mockito.when(userService.enableUser(updateWithMessage.getMessage().getChatId())).thenReturn(EnabledStatus.NEW_USER);

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).welcome(updateWithMessage.getMessage());
    }

    @Test
    void welcome_start() {
        updateWithMessage.getMessage().setText("/start");
        Mockito.when(userService.enableUser(updateWithMessage.getMessage().getChatId())).thenReturn(EnabledStatus.ENABLED_USER);

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).welcome(updateWithMessage.getMessage());
    }

    @Test
    void goodbye() {
        updateWithMessage.getMessage().setText("kicked");
        Mockito.doNothing().when(userService).disableUser(updateWithMessage.getMessage().getChatId());

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).goodbye(updateWithMessage.getMessage());
    }

    @Test
    void earn() {
        updateWithMessage.getMessage().setText("\uD83D\uDC68\u200D\uD83D\uDCBB Заработать");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).earn(updateWithMessage.getMessage());
    }

    @Test
    void promotion() {
        updateWithMessage.getMessage().setText("\uD83D\uDCE2 Продвижение");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).promotion(updateWithMessage.getMessage());
    }

    @Test
    void captchaFail() {
        updateWithMessage.getMessage().setText("captcha fail");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).captchaFail(updateWithMessage.getMessage());
    }

    @Test
    void myOffice() {
        updateWithMessage.getMessage().setText("\uD83D\uDCF1 Мой кабинет");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).myOffice(updateWithMessage.getMessage());
    }

    @Test
    void aboutBot() {
        updateWithMessage.getMessage().setText("\uD83D\uDCDA О боте");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).aboutBot(updateWithMessage.getMessage());
    }

    @Test
    void aboutBotByCallback() {
        updateWithMessage.getMessage().setText("about bot");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).aboutBotByCallback(updateWithMessage.getMessage());
    }

    @Test
    void chat() {
        updateWithMessage.getMessage().setText("chat");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).chat(updateWithMessage.getMessage());
    }

    @Test
    void rules() {
        updateWithMessage.getMessage().setText("rules");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).rules(updateWithMessage.getMessage());
    }

    @Test
    void administration() {
        updateWithMessage.getMessage().setText("admin");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).administration(updateWithMessage.getMessage());
    }

    @Test
    void wantBot() {
        updateWithMessage.getMessage().setText("want bot");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).wantBot(updateWithMessage.getMessage());
    }

    @Test
    void error() {
        updateWithMessage.getMessage().setText("Abra Cadabra");

        spyBot.onUpdateReceived(updateWithMessage);
        Mockito.verify(spyEventsHandler, Mockito.times(1)).error(updateWithMessage.getMessage());
    }
}