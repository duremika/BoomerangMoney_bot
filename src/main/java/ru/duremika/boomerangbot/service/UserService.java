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


    public boolean createOrUpdateUser(Long id) {
        Optional<User> optionalUser = repository.findById(id);
        User user;
        if (optionalUser.isEmpty()) {
            user = createNewUser(id);
            repository.save(user);
            log.info("New user created:\n" + user);
            return true;
        } else {
            user = optionalUser.get();
            if (user.getStatus().equals(Status.BANNED)) {
                log.info("User banned:\n" + user);
            } else if (user.getStatus().equals(Status.DISABLED)) {
                user.setStatus(Status.ENABLED);
                repository.save(user);
                log.info("User already exists:\n" + user);
            }
            return false;
        }
    }

    User createNewUser(Long id) {
        return new User(
                id,
                new Timestamp(System.currentTimeMillis()),
                Status.ENABLED,
                new HashSet<>(),
                new Tasks() {{
                    setId(id);
                }},
                new Balance() {{
                    setId(id);
                }},
                new Earned() {{
                    setId(id);
                }}
        );
    }


    public void disableUser(Long id) {
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
