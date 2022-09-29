package ru.duremika.boomerangbot.annotations;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Filter {
    String[] text() default {};
    String[] command() default {};
    String[] callback() default {};
    String[] specialType() default {};
    Class<? extends ChatMember>[] chatMemberUpdated() default {};

    ChatType chatType() default ChatType.PRIVATE;

    enum ChatType {
        PRIVATE("private"),
        GROUP("group"),
        CHANNEL("channel"),
        SUPERGROUP("supergroup");

        @Getter
        private final String type;

        ChatType(String type) {
            this.type = type;
        }

        public static ChatType fromString(String name){
            if (name != null){
                return Arrays.stream(ChatType.values())
                        .filter(value -> value.type.equals(name))
                        .findFirst()
                        .orElse(null);
            }
            return null;
        }
    }
}