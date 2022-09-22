package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.Task;

import java.util.List;
import java.util.Optional;


public interface TaskRepository extends CrudRepository<Task, Long> {
    Optional<Task> getTaskByOrder_id(String orderId);
}
