package com.drinksco.repository;

import com.drinksco.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findTop10ByOrderByOrderedAtDesc();

	Optional<Order> findByReferenceNumber(String referenceNumber);
}
