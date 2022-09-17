package ru.duremika.boomerangbot.entities;

import lombok.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@NoArgsConstructor
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

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Tasks tasks;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Balance balance;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Earned earned;


    public User(Long id){
        this.id = id;
        this.enabled =true;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.status = Status.INACTIVE;
        this.achievementList = new HashSet<>();
        this.tasks = new Tasks(id, this);
        this.balance = new Balance(id, this);
        this.earned = new Earned(id, this);
    }
}
