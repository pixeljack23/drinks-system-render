package com.drinksco.service;

import com.drinksco.model.Branch;
import com.drinksco.model.CustomerBranchResponse;
import com.drinksco.model.CustomerMenuDrinkResponse;
import com.drinksco.model.CustomerMenuResponse;
import com.drinksco.model.CustomerOrderItemRequest;
import com.drinksco.model.CustomerOrderRequest;
import com.drinksco.model.CustomerReceiptItemResponse;
import com.drinksco.model.CustomerReceiptResponse;
import com.drinksco.model.Order;
import com.drinksco.model.OrderItem;
import com.drinksco.model.StockLevel;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

	private final StockService stockService;
	private final OrderService orderService;
	private final CustomerCatalogService customerCatalogService;

	@Transactional(readOnly = true)
	public List<CustomerBranchResponse> getBranches() {
		return customerCatalogService.getCustomerBranches(stockService.getAllBranches()).stream()
				.map(this::toBranchResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public CustomerMenuResponse getMenu(Long branchId) {
		Branch branch = requireCustomerBranch(branchId);
		List<CustomerMenuDrinkResponse> drinks = customerCatalogService.getCustomerMenuStock(stockService.getStockForBranch(branchId)).stream()
				.filter(stock -> stock.getQuantity() != null && stock.getQuantity() > 0)
				.map(stock -> new CustomerMenuDrinkResponse(
						stock.getDrink().getId(),
						stock.getDrink().getName(),
						stock.getDrink().getCategory(),
						stock.getDrink().getPrice(),
						stock.getQuantity()))
				.toList();

		return new CustomerMenuResponse(toBranchResponse(branch), drinks);
	}

	@Transactional
	public CustomerReceiptResponse placeOrder(CustomerOrderRequest request) {
		Branch branch = requireCustomerBranch(request.branchId());
		List<StockLevel> menuStock = customerCatalogService.getCustomerMenuStock(stockService.getStockForBranch(branch.getId())).stream()
				.filter(stock -> stock.getQuantity() != null && stock.getQuantity() > 0)
				.toList();
		Map<Long, StockLevel> stockByDrinkId = new LinkedHashMap<>();
		for (StockLevel stockLevel : menuStock) {
			stockByDrinkId.put(stockLevel.getDrink().getId(), stockLevel);
		}

		Map<Long, Integer> requestedQuantities = new LinkedHashMap<>();
		for (CustomerOrderItemRequest item : request.items()) {
			if (item.quantity() == null || item.quantity() <= 0) {
				continue;
			}

			StockLevel stockLevel = stockByDrinkId.get(item.drinkId());
			if (stockLevel == null) {
				throw new IllegalArgumentException("Drink not available for the selected branch.");
			}

			int requestedQuantity = requestedQuantities.getOrDefault(item.drinkId(), 0) + item.quantity();
			if (requestedQuantity > stockLevel.getQuantity()) {
				throw new IllegalArgumentException("Only " + stockLevel.getQuantity() + " units of "
						+ stockLevel.getDrink().getName() + " are available at this branch.");
			}
			requestedQuantities.put(item.drinkId(), requestedQuantity);
		}

		if (requestedQuantities.isEmpty()) {
			throw new IllegalArgumentException("Select at least one drink before placing the order.");
		}

		BigDecimal totalAmount = calculateTotal(requestedQuantities, stockByDrinkId);
		BigDecimal amountPaid = request.amountPaid() == null ? totalAmount : request.amountPaid();
		if (amountPaid.compareTo(totalAmount) < 0) {
			throw new IllegalArgumentException("Amount paid cannot be less than the order total.");
		}

		Order order = orderService.placeOrder(branch, request.customerName(), request.customerPhone(), requestedQuantities, amountPaid);
		return toReceiptResponse(order, amountPaid);
	}

	@Transactional(readOnly = true)
	public CustomerReceiptResponse getReceipt(String referenceNumber) {
		return toReceiptResponse(orderService.findByReferenceNumber(referenceNumber), null);
	}

	private BigDecimal calculateTotal(Map<Long, Integer> requestedQuantities, Map<Long, StockLevel> stockByDrinkId) {
		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
			StockLevel stockLevel = stockByDrinkId.get(entry.getKey());
			if (stockLevel == null) {
				throw new IllegalArgumentException("Drink not available for the selected branch.");
			}
			total = total.add(stockLevel.getDrink().getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
		}
		return total;
	}

	private Branch requireCustomerBranch(Long branchId) {
		Branch branch = customerCatalogService.findCustomerBranch(stockService.getAllBranches(), branchId);
		if (branch == null) {
			throw new NoSuchElementException("Branch not found.");
		}
		return branch;
	}

	private CustomerBranchResponse toBranchResponse(Branch branch) {
		return new CustomerBranchResponse(
				branch.getId(),
				branch.getName(),
				customerCatalogService.getBranchDisplayName(branch),
				customerCatalogService.getBranchLabel(branch),
				customerCatalogService.getBranchLocation(branch),
				customerCatalogService.isHeadquarters(branch));
	}

	private CustomerReceiptResponse toReceiptResponse(Order order, BigDecimal amountPaidOverride) {
		BigDecimal amountPaid = amountPaidOverride == null
				? (order.getAmountPaid() == null ? order.getTotalAmount() : order.getAmountPaid())
				: amountPaidOverride;
		BigDecimal balance = amountPaid.subtract(order.getTotalAmount());
		List<CustomerReceiptItemResponse> items = order.getItems().stream()
				.map(this::toReceiptItemResponse)
				.toList();

		return new CustomerReceiptResponse(
				order.getReferenceNumber(),
				order.getStatus(),
				order.getCustomerName(),
				order.getCustomerPhone(),
				toBranchResponse(order.getBranch()),
				order.getOrderedAt(),
				order.getTotalAmount(),
				amountPaid,
				balance,
				items);
	}

	private CustomerReceiptItemResponse toReceiptItemResponse(OrderItem item) {
		return new CustomerReceiptItemResponse(
				item.getDrink().getId(),
				item.getDrink().getName(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getLineTotal());
	}
}
