package com.drinksco.service;

import com.drinksco.model.Branch;
import com.drinksco.model.Drink;
import com.drinksco.model.Order;
import com.drinksco.model.OrderItem;
import com.drinksco.repository.BranchRepository;
import com.drinksco.repository.DrinkRepository;
import com.drinksco.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final BranchRepository branchRepository;
	private final DrinkRepository drinkRepository;
	private final StockService stockService;

	@Transactional
	public Order placeOrder(Long branchId, String customerName, String customerPhone, Map<Long, Integer> quantities) {
		return placeOrder(branchId, customerName, customerPhone, quantities, null);
	}

	@Transactional
	public Order placeOrder(Long branchId, String customerName, String customerPhone, Map<Long, Integer> quantities, BigDecimal amountPaid) {
		Branch branch = branchRepository.findById(branchId)
				.orElseThrow(() -> new IllegalArgumentException("Branch not found."));
		return placeOrder(branch, customerName, customerPhone, quantities, amountPaid);
	}

	@Transactional
	public Order placeOrder(Branch branch, String customerName, String customerPhone, Map<Long, Integer> quantities) {
		return placeOrder(branch, customerName, customerPhone, quantities, null);
	}

	@Transactional
	public Order placeOrder(Branch branch, String customerName, String customerPhone, Map<Long, Integer> quantities, BigDecimal amountPaid) {
		List<OrderItem> items = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;

		for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
			Integer quantity = entry.getValue();
			if (quantity == null || quantity <= 0) {
				continue;
			}

			Drink drink = drinkRepository.findById(entry.getKey())
					.orElseThrow(() -> new IllegalArgumentException("Drink not found."));
			stockService.adjustStock(branch.getId(), drink.getId(), -quantity);

			BigDecimal lineTotal = drink.getPrice().multiply(BigDecimal.valueOf(quantity));
			OrderItem item = OrderItem.builder()
					.drink(drink)
					.quantity(quantity)
					.unitPrice(drink.getPrice())
					.lineTotal(lineTotal)
					.build();
			items.add(item);
			total = total.add(lineTotal);
		}

		if (items.isEmpty()) {
			throw new IllegalArgumentException("Add at least one drink to place an order.");
		}

		Order order = Order.builder()
				.branch(branch)
				.customerName(customerName == null || customerName.isBlank() ? "Walk-in Customer" : customerName)
				.customerPhone(customerPhone == null || customerPhone.isBlank() ? "Not provided" : customerPhone)
				.referenceNumber(buildReferenceNumber())
				.status("CONFIRMED")
				.totalAmount(total)
				.amountPaid(amountPaid == null ? total : amountPaid)
				.build();

		for (OrderItem item : items) {
			item.setOrder(order);
		}
		order.setItems(items);

		return orderRepository.save(order);
	}

	@Transactional(readOnly = true)
	public Order findByReferenceNumber(String referenceNumber) {
		return orderRepository.findByReferenceNumber(referenceNumber)
				.orElseThrow(() -> new NoSuchElementException("Order not found."));
	}

	private String buildReferenceNumber() {
		return "DC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
	}
}
