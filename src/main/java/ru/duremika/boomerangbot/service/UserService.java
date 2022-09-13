package ru.duremika.boomerangbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.*;
import ru.duremika.boomerangbot.exception.UserBannedException;
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

    public Optional<User> findUser(Long id){
        return repository.findById(id);
    }

    public boolean createOrUpdateUser(Long id) throws UserBannedException {
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
                throw new UserBannedException();
            } else if (!user.isEnabled()) {
                user.setEnabled(true);
                repository.save(user);
                log.info("User already exists:\n" + user);
            }
            return false;
        }
    }

    User createNewUser(Long id) {
        return new User(
                id,
                true,
                new Timestamp(System.currentTimeMillis()),
                Status.INACTIVE,
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
                    user.setEnabled(false);
                    repository.save(user);
                    log.info("User disabled:\n" + user);
                },
                () -> log.info("User with id " + id + " not exists")
        );
    }

}
