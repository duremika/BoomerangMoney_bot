package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.*;
import ru.duremika.boomerangbot.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
public class UserService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Optional<User> findUser(Long id) {
        return repository.findById(id);
    }

    public EnabledStatus enableUser(Long id) {
        Optional<User> optionalUser = repository.findById(id);
        User user;
        if (optionalUser.isEmpty()) {
            user = new User(id);
            repository.save(user);
            log.info("New user created: " + user);
            return EnabledStatus.NEW_USER;
        } else {
            user = optionalUser.get();
            if (user.getStatus().equals(Status.BANNED)){
                log.info("User banned: " + user);
                return EnabledStatus.BANNED_USER;
            } else if (!user.isEnabled()){
                user.setEnabled(true);
                repository.save(user);
                log.info("User enabled: " + user);
                return EnabledStatus.DISABLED_USER;
            } else {
                log.info("User already enabled: " + user);
                return EnabledStatus.ENABLED_USER;
            }
        }
    }

    public void disableUser(Long id) {
        repository.findById(id).ifPresentOrElse(
                (user) -> {
                    user.setEnabled(false);
                    repository.save(user);
                    log.info("User disabled:\n" + user);
                },
                () -> log.info("User with id " + id + " not exists")
        );
    }

}
