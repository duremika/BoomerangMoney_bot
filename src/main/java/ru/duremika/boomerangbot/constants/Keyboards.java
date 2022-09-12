package ru.duremika.boomerangbot.constants;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

public class Keyboards {
    public final static ReplyKeyboardMarkup mainReplyKeyboardMarkup = ReplyKeyboardMarkup.builder()
            .keyboard(
                    List.of(new KeyboardRow(
                                    List.of(KeyboardButton.builder().text("\uD83D\uDC68\u200D\uD83D\uDCBB Заработать").build(),
                                            KeyboardButton.builder().text("\uD83D\uDCE2 Продвижение").build())
                            ),
                            new KeyboardRow(
                                    List.of(KeyboardButton.builder().text("\uD83D\uDCF1 Мой кабинет").build(),
                                            KeyboardButton.builder().text("\uD83D\uDC65 Партнёры").build())
                            ),
                            new KeyboardRow(
                                    List.of(KeyboardButton.builder().text("\uD83D\uDCDA О боте").build())
                            )
                    )
            )
            .resizeKeyboard(true)
            .build();
}
