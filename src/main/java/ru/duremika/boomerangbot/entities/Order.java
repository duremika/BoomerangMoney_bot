package ru.duremika.boomerangbot.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;
    private int amount;
    private int performed;

    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "author_id")
    @ToString.Exclude
    private User author;

    @OneToMany(mappedBy = "order")
    private List<Task> tasks;

    public enum Type {
        POST,
        CHANNEL,
        BOT,
        GROUP,
        EXTENDED_TASK,
        BONUS
    }
}
