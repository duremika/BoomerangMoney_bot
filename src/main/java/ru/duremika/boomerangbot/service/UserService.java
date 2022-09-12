package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.*;
import ru.duremika.boomerangbot.repository.UserRepository;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Optional;

@Slf4j
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }


    public boolean createOrUpdateUser(Long id){
        boolean isNew;

        Optional<User> optionalUser = repository.findById(id);
        User user;
        if (optionalUser.isEmpty()){
            isNew = true;
            user = new User();
            user.setId(id);
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            user.setAchievementList(new HashSet<>());
            user.setTasks(new Tasks(){{setId(id);}});
            user.setBalance(new Balance(){{setId(id);}});
            user.setEarned(new Earned(){{setId(id);}});
            user.setStatus(Status.INACTIVE);
            repository.save(user);
            log.info("New user created:\n" + user);
        } else {
            isNew = false;
            user = optionalUser.get();
            user.setStatus(Status.INACTIVE);
            repository.save(user);
            log.info("User already created:\n" + user);
        }
        return isNew;
    }

    public void disableUser(Long id){
        repository.findById(id).ifPresentOrElse(
                (user) -> {
                    user.setStatus(Status.DISABLED);
                    repository.save(user);
                    log.info("User disabled:\n" + user);
                },
                () -> log.info("User with id " + id + " not exists")
        );
    }

}
