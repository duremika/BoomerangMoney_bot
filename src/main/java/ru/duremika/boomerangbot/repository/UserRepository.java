package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.User;


public interface UserRepository extends CrudRepository<User, Long> {
}
