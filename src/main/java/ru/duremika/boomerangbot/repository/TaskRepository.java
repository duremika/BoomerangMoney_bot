package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;

import java.util.Optional;


public interface TaskRepository extends CrudRepository<Task, Long> {
    Optional<Task> getTaskByOrder_idAndUser(String order_id, User user);
}