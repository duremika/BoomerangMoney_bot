package ru.duremika.boomerangbot.entities;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Tasks {
    @Id
    private Long id;
    private int channels;
    private int groups;
    private int bots;
    private int views;
    private int extendedTask;
    private int bonuses;

    public Tasks(){

    }
}
