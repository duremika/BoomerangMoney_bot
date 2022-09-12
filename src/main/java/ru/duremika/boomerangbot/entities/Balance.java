package ru.duremika.boomerangbot.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Balance {
    @Id
    private Long id;
    private int main;
    private int advertising;
    private int toppedUp;
    private int spent;
    private int output;

    public Balance(){

    }
}
