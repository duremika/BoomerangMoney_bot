package ru.duremika.boomerangbot.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
public class Balance {
    @Id
    private Long user_id;
    private int main;
    private int advertising;
    private int toppedUp;
    private int spent;
    private int output;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    public Balance(Long id, User user) {
        this.user_id = id;
        this.user = user;
    }
}
