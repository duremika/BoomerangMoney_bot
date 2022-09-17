package ru.duremika.boomerangbot.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@Entity
@NoArgsConstructor
public class Earned {
    @Id
    private Long user_id;
    private int total;
    private int frozen;
    private int await;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private User user;

    public Earned(Long id, User user) {
        this.user_id = id;
        this.user = user;
    }
}
