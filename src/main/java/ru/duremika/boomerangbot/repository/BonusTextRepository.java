package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import ru.duremika.boomerangbot.entities.BonusText;


@Repository
public interface BonusTextRepository extends CrudRepository<BonusText, String> {
}
