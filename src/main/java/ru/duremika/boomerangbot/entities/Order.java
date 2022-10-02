package ru.duremika.boomerangbot.entities;

import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    String link;
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
