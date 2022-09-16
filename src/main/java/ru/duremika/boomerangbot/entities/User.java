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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "achievement_list", joinColumns=@JoinColumn(name="user_id"))
    private Set<Integer> achievementList;

    @OneToOne(cascade = CascadeType.ALL)
    private Tasks tasks;

    @OneToOne(cascade = CascadeType.ALL)
    private Balance balance;

    @OneToOne(cascade = CascadeType.ALL)
    private Earned earned;
}
