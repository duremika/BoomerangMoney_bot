package ru.duremika.boomerangbot.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.duremika.boomerangbot.entities.Order;
import ru.duremika.boomerangbot.entities.User;

import java.util.List;


public interface OrderRepository extends CrudRepository<Order, Long> {
    List<Order> getAllByAuthor(User author);

    @Query(" select o " +
            "from Order o " +
            "where o.performed < o.amount and o.id not in " +
                "(select t.order.id from Task t" +
                " where t.user.id = ?1) ")
    List<Order> getOrdersByPerformedLessThanAmount(Long id);

    @Query(" select o " +
            "from Order o " +
            "where o.type = ?2 and o.performed < o.amount and o.id not in " +
            "(select t.order.id from Task t" +
            " where t.user.id = ?1) ")
    List<Order> getOrdersByPerformedLessThanAmountWithType(Long id, Order.Type type);
}
