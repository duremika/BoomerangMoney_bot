package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.repository.TaskRepository;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository repository;
    public Task getTaskByOrderId(Long orderId, User user){
        return repository.getTaskByOrder_idAndUser(orderId, user).orElse(null);
    }

    public void add(Task task) {
        repository.save(task);
    }
}
