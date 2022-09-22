package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Balance;
import ru.duremika.boomerangbot.entities.User;


public interface BalanceRepository extends CrudRepository<Balance, Long> {
}
