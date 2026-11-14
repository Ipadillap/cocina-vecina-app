package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Order, Long> {
}
