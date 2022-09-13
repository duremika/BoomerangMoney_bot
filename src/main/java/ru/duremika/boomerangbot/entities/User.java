package ru.duremika.boomerangbot.entities;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private Timestamp createdAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(name = "achievement_list")
    private Set<Integer> achievementList;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Tasks tasks;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Balance balance;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private Earned earned;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", status=" + status +
                '}';
    }
}
