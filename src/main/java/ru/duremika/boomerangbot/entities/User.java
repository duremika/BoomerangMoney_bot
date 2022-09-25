package ru.duremika.boomerangbot.entities;

import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private String lastMessage;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "achievement_list", joinColumns=@JoinColumn(name="user_id"))
    private Set<Integer> achievementList;

    @OneToMany(mappedBy = "user")
    @LazyCollection(LazyCollectionOption.FALSE)
    @ToString.Exclude
    private List<Task> tasks;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Balance balance;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private Earned earned;

    @OneToMany(mappedBy = "author", fetch = FetchType.EAGER)
    @ToString.Exclude
    private List<Order> orders;


    public User(Long id){
        this.id = id;
        this.enabled =true;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.status = Status.INACTIVE;
        this.achievementList = new HashSet<>();
        this.tasks = new ArrayList<>();
        this.balance = new Balance(id, this);
        this.earned = new Earned(id, this);
    }
}
