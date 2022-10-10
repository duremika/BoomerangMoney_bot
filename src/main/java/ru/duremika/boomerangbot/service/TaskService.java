package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.repository.TaskRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository repository;
    public Task getTaskByOrderId(Long orderId, User user){
        List<Task> tasks = repository.getTaskByOrder_idAndUser(orderId, user);
        return tasks.size() > 0 ? tasks.get(0) : null;
    }

    public void add(Task task) {
        repository.save(task);
    }
}
