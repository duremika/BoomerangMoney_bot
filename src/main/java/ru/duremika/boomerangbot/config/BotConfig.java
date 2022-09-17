package ru.duremika.boomerangbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class BotConfig {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String token;

    private final String infoChannelId = "-1001697520335";
    private final String viewsChannelId = "-1001718302900";
}
