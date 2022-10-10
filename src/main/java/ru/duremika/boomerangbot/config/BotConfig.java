package ru.duremika.boomerangbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.text.DecimalFormat;

@Configuration
@Getter
public class BotConfig {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    private final String infoChannelId = "-1001697520335";
    private final String viewsChannelId = "-1001718302900";
    private final DecimalFormat decimalFormat = new DecimalFormat("0.00");


    private final float postOrderPrice = 0.05f;
    private final int minPostOrderAmount = 50;
    private final float postViewPrice = 0.03f;


    private final float channelOrderPrice = 0.25f;
    private final int minChannelOrderAmount = 2;
    private final float channelSubscribePrice = 0.2f;

    private final float groupOrderPrice = 0.4f;
    private final int minGroupOrderAmount = 2;
    private final float groupJoinPrice = 0.3f;


    private final float botOrderPrice = 0.25f;
    private final int minBotOrderAmount = 10;
    private final float botStartPrice = 0.15f;

    private final int inviteFriendPrice = 1;

    private final float bonusOrderPrice = 200.0f;
    private final float bonusReceivePrice = 0.021f;
}
