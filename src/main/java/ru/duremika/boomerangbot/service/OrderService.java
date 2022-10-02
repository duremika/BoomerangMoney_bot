package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order getOrderById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Order> getUserOrders(User author) {
        return repository.getAllByAuthor(author);
    }

    public List<Order> getAvailableOrders(Long id){
        return repository.getOrdersByPerformedLessThanAmount(id);
    }

    public List<Order> getAvailableOrders(Long id, Order.Type type){
        return repository.getOrdersByPerformedLessThanAmountWithType(id, type);
    }

    public Order add(Order order) {
        return repository.save(order);
    }
}
