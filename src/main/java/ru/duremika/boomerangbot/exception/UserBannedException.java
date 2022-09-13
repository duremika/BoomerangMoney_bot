package ru.duremika.boomerangbot.exception;

public class UserBannedException extends Exception{
    public UserBannedException() {
        super("Can not enable a banned user");
    }
}
