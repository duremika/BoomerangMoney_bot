package ru.duremika.boomerangbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

@Component
@Getter
public class BotConfig {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    private final String infoChannelId = "-1001697520335";
    private final String viewsChannelId = "-1001718302900";
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");


    private final float postOrderPrice = 0.05f;
    private final int minPostOrderAmount = 50;
    private final float postViewPrice = 0.03f;


    private final float channelOrderPrice = 0.25f;
    private final int minChannelOrderAmount = 2;
    private final float channelSubscribePrice = 0.2f;

    private final float groupJoinPrice = 0.3f;

    private final float botStartPrice = 0.15f;

    private final int inviteFriendPrice = 1;

    private final float bonusPrice = 0.021f;
}
