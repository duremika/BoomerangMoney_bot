package ru.duremika.boomerangbot.keyboards;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.duremika.boomerangbot.config.BotConfig;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

@Component
public class Keyboards {
    private final BotConfig config;
    private final DecimalFormat decimalFormat;
    private final float postViewPrice;

    public Keyboards(BotConfig config) {
        this.config = config;
        decimalFormat = config.getDecimalFormat();
        postViewPrice = config.getPostViewPrice();
    }


    public ReplyKeyboardMarkup mainReplyKeyboardMarkup() {
        return ReplyKeyboardMarkup.builder()
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
    }

    public InlineKeyboardMarkup earnInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83E\uDD16 Перейти в бота +" + decimalFormat.format(config.getBotStartPrice()) + "₽") {{
                            setCallbackData("earn_bot");
                        }}
                ))
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDCE2 Подписаться на канал +" + decimalFormat.format(config.getChannelSubscribePrice()) + "₽") {{
                            setCallbackData("earn_channel");
                        }}
                ))
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDC64 Вступить в группу +" + decimalFormat.format(config.getGroupJoinPrice()) + "₽") {{
                            setCallbackData("earn_group");
                        }}
                ))
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDC41 Смотреть посты +" + decimalFormat.format(config.getPostViewPrice()) + "₽") {{
                            setUrl("https://t.me/boomerang_money_viewer");
                        }}
                ))
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDE4B\uD83C\uDFFB\u200D♂️ Пригласить друга +" + config.getInviteFriendPrice() + "₽") {{
                            setCallbackData("earn_invite");
                        }}
                ))
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDD0E Доп. задания") {{
                            setCallbackData("earn_additional");
                        }},
                        new InlineKeyboardButton("\uD83C\uDF81 Бонус +" + config.getBonusPrice() + "₽") {{
                            setCallbackData("earn_bonus");
                        }}
                )).build();
    }

    public InlineKeyboardMarkup promotionInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
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
                    setCallbackData("auto_views");
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
                    setCallbackData("bonus_sponsor");
                }}))
                .build();
    }

    public InlineKeyboardMarkup postInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDC41 Добавить пост") {{
                    setCallbackData("add_post");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("⏱ Активные заказы") {{
                    setCallbackData("active_post_orders");
                }}, new InlineKeyboardButton("✅ Завершённые заказы") {{
                    setCallbackData("completed_post_orders");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("promotion");
                }}))
                .build();
    }

    public InlineKeyboardMarkup channelInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCE2 Добавить канал") {{
                    setCallbackData("add_channel");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("⏱ Активные заказы") {{
                    setCallbackData("active_channel_orders");
                }}, new InlineKeyboardButton("✅ Завершённые заказы") {{
                    setCallbackData("completed_channel_orders");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("promotion");
                }}))
                .build();
    }

    public InlineKeyboardMarkup groupInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDC65 Добавить группу") {{
                    setCallbackData("add_group");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("⏱ Активные заказы") {{
                    setCallbackData("active_group_orders");
                }}, new InlineKeyboardButton("✅ Завершённые заказы") {{
                    setCallbackData("completed_group_orders");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("promotion");
                }}))
                .build();
    }

    public InlineKeyboardMarkup captchaInlineKeyboard(int result) {
        int[] buttons = generateButtonsForCaptcha(result);
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        new InlineKeyboardButton(String.valueOf(buttons[0])) {{
                            setCallbackData(buttons[0] == result ? "captcha_success" : "captcha_fail");
                        }},
                        new InlineKeyboardButton(String.valueOf(buttons[1])) {{
                            setCallbackData(buttons[1] == result ? "captcha_success" : "captcha_fail");
                        }},
                        new InlineKeyboardButton(String.valueOf(buttons[2])) {{
                            setCallbackData(buttons[2] == result ? "captcha_success" : "captcha_fail");
                        }}
                )).build();
    }

    private int[] generateButtonsForCaptcha(int result) {
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

    public InlineKeyboardMarkup addPostToInfoChannelInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDC41 Канал с просмотрами") {{
                    setUrl("https://t.me/boomerang_money_viewer");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Перейти в boomerang_money_bot") {{
                    setUrl("https://t.me/boomerang_money_bot");
                }}))
                .build();
    }

    public InlineKeyboardMarkup addChannelToInfoChannelInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Перейти в boomerang_money_bot") {{
                    setUrl("https://t.me/boomerang_money_bot");
                }}))
                .build();
    }

    public InlineKeyboardMarkup myOfficeInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(
                        new InlineKeyboardButton("\uD83D\uDCB3 Пополнить") {{
                            setCallbackData("top_up");
                        }},
                        new InlineKeyboardButton("\uD83D\uDCB8 Вывести") {{
                            setCallbackData("output");
                        }}
                ))
                .keyboardRow(List.of(new InlineKeyboardButton("♻️ Конвертировать") {{
                    setCallbackData("convert");
                }}))
                .build();
    }

    public InlineKeyboardMarkup aboutBotInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
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
                                         setCallbackData("want_bot");
                                     }}
                ))
                .build();
    }

    public InlineKeyboardMarkup chatInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCAC Чат \uD83C\uDDF7\uD83C\uDDFA") {{
                    setUrl("https://t.me/boomerang_money_chat");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("about_bot");
                }}))
                .build();
    }

    public InlineKeyboardMarkup rulesInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("about_bot");
                }}))
                .build();
    }


    public InlineKeyboardMarkup administrationInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCDD Написать") {{
                    setUrl("https://t.me/x_MaksOn_x");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("about_bot");
                }}))
                .build();
    }

    public InlineKeyboardMarkup wantBotInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCDD Написать") {{
                    setUrl("https://t.me/x_MaksOn_x");
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("about_bot");
                }}))
                .build();
    }

    public ReplyKeyboardMarkup toMainReplyKeyboardMarkup() {
        return ReplyKeyboardMarkup.builder()
                .keyboardRow(new KeyboardRow(List.of(new KeyboardButton("◀️ На главную"))))
                .resizeKeyboard(true)
                .build();
    }

    public InlineKeyboardMarkup backToEarnInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("earn");
                }}))
                .build();
    }

    public InlineKeyboardMarkup postViewedInlineKeyboard(Long orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDCB0 +" + decimalFormat.format(postViewPrice) + "₽") {{
                    setCallbackData("post_viewed " + orderId);
                }}))
                .build();
    }


    public InlineKeyboardMarkup channelEarnInlineKeyboard(String link, Long orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("1️⃣  Перейти к каналу") {{
                    setUrl(link);
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("2️⃣  Проверить подписку") {{
                    setCallbackData("check_subscribe " + orderId);
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("▶️ Пропустить задание") {{
                    setCallbackData("ignore_subscribe " + orderId);
                }}))
                .keyboardRow(List.of(new InlineKeyboardButton("\uD83D\uDD19 Назад") {{
                    setCallbackData("earn");
                }}))
                .build();
    }

    public InlineKeyboardMarkup nextTaskChannelInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(new InlineKeyboardButton("▶️ Следующее задание") {{
                    setCallbackData("next_task_channel");
                }}))
                .build();
    }
}
