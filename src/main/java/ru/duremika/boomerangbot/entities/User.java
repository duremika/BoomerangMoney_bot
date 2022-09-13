package ru.duremika.boomerangbot.entities;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private boolean enabled;
    private Timestamp createdAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(name = "achievement_list")
    private Set<Integer> achievementList;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private Tasks tasks;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private Balance balance;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    private Earned earned;
}
