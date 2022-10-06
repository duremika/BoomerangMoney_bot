package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.*;
import ru.duremika.boomerangbot.repository.BalanceRepository;
import ru.duremika.boomerangbot.repository.EarnedRepository;
import ru.duremika.boomerangbot.repository.UserRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final BalanceRepository balanceRepository;
    private final EarnedRepository earnedRepository;


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
            if (user.getStatus().equals(Status.BANNED)) {
                log.info("User banned: " + user);
                return EnabledStatus.BANNED_USER;
            } else if (!user.isEnabled()) {
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

    public void saveLastMessage(Long id, String text) {
        findUser(id).ifPresentOrElse(user -> {
                    user.setLastMessage(text);
                    repository.save(user);
                },
                () -> log.error("last message: '" + text + "' did not saved"));
    }

    public String getLastMessage(Long id) {
        String text = null;
        Optional<User> optionalUser = findUser(id);
        if (optionalUser.isPresent()) {
            text = optionalUser.get().getLastMessage();
        }
        return text;
    }

    public void writeOfFromAdvertising(Long id, float writeOfAmount) {
        balanceRepository.findById(id)
                .ifPresentOrElse(balance -> {
                            balance.setAdvertising(balance.getAdvertising() - writeOfAmount);
                            balanceRepository.save(balance);
                        },
                        () -> log.error("write-off in the amount of " + writeOfAmount + " ₽ from user " + id + " did not pass")
                );
    }

    public void replenishMainBalance(Long id, float replenishAmount) {
        balanceRepository.findById(id)
                .ifPresentOrElse(balance -> {
                            balance.setMain(balance.getMain() + replenishAmount);
                            balanceRepository.save(balance);
                        },
                        () -> log.error("replenish the main balance in the amount of " + replenishAmount + " ₽ from user " + id + " did not pass")
                );
        earnedRepository.findById(id)
                .ifPresentOrElse(earned -> {
                            earned.setTotal(earned.getTotal() + replenishAmount);
                            earnedRepository.save(earned);
                        },
                        () -> log.error("replenish the main balance in the amount of " + replenishAmount + " ₽ from user " + id + " did not pass")
                );
    }

    public enum EnabledStatus {
        NEW_USER,
        DISABLED_USER,
        ENABLED_USER,
        BANNED_USER
    }
}
