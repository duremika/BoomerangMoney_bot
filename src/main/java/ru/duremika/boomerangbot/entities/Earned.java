package ru.duremika.boomerangbot.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Earned {
    @Id
    private Long id;
    private int total;
    private int frozen;
    private int await;

    public Earned() {
    }
}
