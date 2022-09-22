package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Balance;
import ru.duremika.boomerangbot.entities.Earned;


public interface EarnedRepository extends CrudRepository<Earned, Long> {
}
