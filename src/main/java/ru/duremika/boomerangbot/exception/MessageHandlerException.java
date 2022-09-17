package ru.duremika.boomerangbot.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MessageHandlerException extends Exception{
    public MessageHandlerException(String errorMessage){
        super(errorMessage);
    }
}
