package ru.duremika.boomerangbot.service;

import org.springframework.stereotype.Service;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;
import ru.duremika.boomerangbot.repository.OrderRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order getOrderById(String id) {
        return repository.findById(id).orElse(null);
    }

    public List<Order> getActiveUserOrders(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getAmount() > order.getPerformed())
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());
    }

    public List<Order> getCompletedUserOrders(List<Order> orders) {
        return orders.stream()
                .filter(order -> order.getAmount() <= order.getPerformed())
                .sorted(Comparator.comparingInt(Order::getMidInViewsChannel))
                .collect(Collectors.toList());
    }

    public List<Order> getUserOrders(User author) {
        return repository.getAllByAuthor(author);
    }

    public void add(Order order) {
        repository.save(order);
    }
}
