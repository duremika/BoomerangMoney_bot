package ru.duremika.boomerangbot.repository;

import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;

import java.util.List;


public interface OrderRepository extends CrudRepository<Order, String> {
    List<Order> getAllByAuthor(User author);
}
