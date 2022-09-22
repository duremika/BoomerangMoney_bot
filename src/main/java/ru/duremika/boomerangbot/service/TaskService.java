package ru.duremika.boomerangbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.Task;
import ru.duremika.boomerangbot.repository.TaskRepository;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository repository;
    public Task getTaskByOrderId(String orderId){
        return repository.getTaskByOrder_id(orderId).orElse(null);
    }

    public void add(Task task) {
        repository.save(task);
    }
}
