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
    private float main;
    private float advertising;
    private float toppedUp;
    private float spent;
    private float output;

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
