package ru.duremika.boomerangbot.constants;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;
import java.util.Random;

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

    public final static InlineKeyboardMarkup promotionInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(
                    new InlineKeyboardButton("\uD83D\uDC41 Пост") {{
                        setCallbackData("post");
                    }},
                    new InlineKeyboardButton("\uD83D\uDC65 Канал") {{
                        setCallbackData("channel");
                    }}
            ))
            .keyboardRow(List.of(
                    new InlineKeyboardButton("\uD83E\uDD16 Бота") {{
                        setCallbackData("bot");
                    }},
                    new InlineKeyboardButton("\uD83D\uDC65 Группу") {{
                        setCallbackData("group");
                    }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDC40 Автопросмотры") {{
                setCallbackData("auto views");
            }}))
            .keyboardRow(List.of(
                    new InlineKeyboardButton("\uD83D\uDCCC Закреп") {{
                        setCallbackData("pinned");
                    }},
                    new InlineKeyboardButton("✉️ Рассылка") {{
                        setCallbackData("mailing");
                    }}
            ))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83C\uDFAB Стать спонсором бонусов") {{
                setCallbackData("bonus sponsor");
            }}))
            .build();

    public static InlineKeyboardMarkup captchaInlineKeyboard(int result) {
        int[] buttons = generateButtonsForCaptcha(result);
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        new InlineKeyboardButton(String.valueOf(buttons[0])) {{
                            setCallbackData(buttons[0]==result ? "captcha success" : "captcha fail");
                        }},
                        new InlineKeyboardButton(String.valueOf(buttons[1])) {{
                            setCallbackData(buttons[1]==result ? "captcha success" : "captcha fail");
                        }},
                        new InlineKeyboardButton(String.valueOf(buttons[2])) {{
                            setCallbackData(buttons[2]==result ? "captcha success" : "captcha fail");
                        }}
                )).build();
    }

    private static int[] generateButtonsForCaptcha(int result) {
        int[] buttons = new int[3];
        Random random = new Random();
        int position = random.nextInt(3);
        buttons[0] = position == 0 ? result :
                position == 1 ? result - 1 : result - 2;
        buttons[1] = position == 0 ? result + 1 :
                position == 1 ? result : result - 1;
        buttons[2] = position == 0 ? result + 2 :
                position == 1 ? result + 1 : result;
        return buttons;
    }

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
                                     setUrl("https://t.me/boomerang_money_info");
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

    public final static InlineKeyboardMarkup chatInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCAC Чат \uD83C\uDDF7\uD83C\uDDFA") {{
                setUrl("https://t.me/boomerang_money_chat");
            }}))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                setCallbackData("about bot");
            }}))
            .build();

    public final static InlineKeyboardMarkup rulesInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                setCallbackData("about bot");
            }}))
            .build();

    public final static InlineKeyboardMarkup administrationInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCDD Написать") {{
                setUrl("https://t.me/x_MaksOn_x");
            }}))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                setCallbackData("about bot");
            }}))
            .build();

    public final static InlineKeyboardMarkup wantBotInlineKeyboard = InlineKeyboardMarkup.builder()
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCDD Написать") {{
                setUrl("https://t.me/x_MaksOn_x");
            }}))
            .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                setCallbackData("about bot");
            }}))
            .build();
}
