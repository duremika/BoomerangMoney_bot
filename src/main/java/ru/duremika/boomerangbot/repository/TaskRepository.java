package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;

import java.util.List;


public interface TaskRepository extends CrudRepository<Task, Long> {
    List<Task> getTaskByOrder_idAndUser(Long order_id, User user);
}
