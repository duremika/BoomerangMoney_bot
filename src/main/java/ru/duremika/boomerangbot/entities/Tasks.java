package ru.duremika.boomerangbot.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
public class Tasks {
    @Id
    private Long user_id;
    private int channels;
    private int groups;
    private int bots;
    private int views;
    private int extendedTask;
    private int bonuses;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    public Tasks(Long id, User user) {
        this.user_id = id;
        this.user = user;
    }
}
