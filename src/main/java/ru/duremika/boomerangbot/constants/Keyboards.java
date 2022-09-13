package ru.duremika.boomerangbot.constants;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

public class Keyboards {
    public final static ReplyKeyboardMarkup mainReplyKeyboardMarkup = ReplyKeyboardMarkup.builder()
            .keyboardRow(new KeyboardRow(
                    List.of(new KeyboardButton("\uD83D\uDC68\u200D\uD83D\uDCBB Заработать"),
                            new KeyboardButton("\uD83D\uDCE2 Продвижение"))
            ))
            .keyboardRow(new KeyboardRow(
                    List.of(new KeyboardButton("\uD83D\uDCF1 Мой кабинет"),
                            new KeyboardButton("\uD83D\uDC65 Партнёры"))
            ))
            .keyboardRow(new KeyboardRow(
                    List.of(new KeyboardButton("\uD83D\uDCDA О боте"))
            ))
            .resizeKeyboard(true)
            .build();

    public final static InlineKeyboardMarkup myOfficeInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(
                    new InlineKeyboardButton("\uD83D\uDCB3 Пополнить") {{
                        setCallbackData("top up");
                    }},
                    new InlineKeyboardButton("\uD83D\uDCB8 Вывести") {{
                        setCallbackData("output");
                    }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("♻️ Конвертировать") {{
                setCallbackData("convert");
            }}))
            .build();

    public final static InlineKeyboardMarkup aboutBotInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(new InlineKeyboardButton("⭐️ Аукцион ⭐️") {{
                                     setUrl("https://t.me/BotFather");
                                 }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCCA Статистика") {{
                                     setCallbackData("statistic");
                                 }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD0A Новости") {{
                                     setUrl("https://t.me/BotFather");
                                 }}
            ))
            .keyboardRow(List.of(
                    new InlineKeyboardButton("\uD83D\uDC65 Чат") {{
                        setCallbackData("chat");
                    }},
                    new InlineKeyboardButton("\uD83D\uDEB8 Правила") {{
                        setCallbackData("rules");
                    }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDC64 Администрация") {{
                                     setCallbackData("admin");
                                 }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDE80 Хочу такого-же бота!") {{
                                     setCallbackData("want bot");
                                 }}
            ))
            .build();


}
