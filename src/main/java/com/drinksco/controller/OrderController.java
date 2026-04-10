package com.drinksco.controller;

import com.drinksco.model.Branch;
import com.drinksco.model.Order;
import com.drinksco.model.StockLevel;
import com.drinksco.service.OrderService;
import com.drinksco.service.StockService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private static final List<String> CUSTOMER_BRANCH_ORDER = List.of(
			"Headquarters",
			"Mombasa",
			"Nakuru",
			"Kisumu");

	private final StockService stockService;
	private final OrderService orderService;

	@GetMapping("/")
	public String root() {
		return "redirect:/order";
	}

	@GetMapping("/order")
	public String orderStart(Model model) {
		model.addAttribute("pageTitle", "Choose Branch");
		model.addAttribute("branches", getCustomerBranches(stockService.getAllBranches()));
		return "customer";
	}

	@GetMapping("/order/{branchId:\\d+}")
	public String orderMenu(@PathVariable Long branchId, Model model) {
		Branch selectedBranch = findCustomerBranch(branchId);
		if (selectedBranch == null) {
			return "redirect:/order";
		}

		List<StockLevel> availableGoods = stockService.getStockForBranch(branchId).stream()
				.filter(stock -> stock.getQuantity() != null && stock.getQuantity() > 0)
				.toList();

		model.addAttribute("pageTitle", "Drinks Menu");
		model.addAttribute("selectedBranch", selectedBranch);
		model.addAttribute("selectedBranchLabel", getBranchLabel(selectedBranch));
		model.addAttribute("stockLevels", availableGoods);
		return "branch-goods";
	}

	@PostMapping("/order/{branchId:\\d+}")
	public String placeOrder(
			@PathVariable Long branchId,
			@RequestParam String customerName,
			@RequestParam String customerPhone,
			@RequestParam(required = false) BigDecimal amountPaid,
			@RequestParam Map<String, String> formData,
			RedirectAttributes redirectAttributes) {

		Branch selectedBranch = findCustomerBranch(branchId);
		if (selectedBranch == null) {
			return "redirect:/order";
		}

		Map<Long, Integer> quantities = parseQuantities(formData);
		BigDecimal requestedTotal = calculateRequestedTotal(branchId, quantities);
		BigDecimal safeAmountPaid = amountPaid == null ? BigDecimal.ZERO : amountPaid;

		if (requestedTotal.compareTo(BigDecimal.ZERO) <= 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "Select at least one drink before placing the order.");
			return "redirect:/order/" + branchId;
		}

		if (safeAmountPaid.compareTo(requestedTotal) < 0) {
			redirectAttributes.addFlashAttribute("errorMessage", "Amount paid cannot be less than the order total.");
			return "redirect:/order/" + branchId;
		}

		try {
			Order order = orderService.placeOrder(branchId, customerName, customerPhone, quantities, safeAmountPaid);
			redirectAttributes.addFlashAttribute("order", order);
			redirectAttributes.addFlashAttribute("amountPaid", safeAmountPaid);
			redirectAttributes.addFlashAttribute("balance", safeAmountPaid.subtract(order.getTotalAmount()));
			redirectAttributes.addFlashAttribute("branchLabel", getBranchLabel(order.getBranch()));
			return "redirect:/order/confirmation";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/order/" + branchId;
		}
	}

	@GetMapping("/order/confirmation")
	public String confirmation(Model model) {
		if (!model.containsAttribute("order")) {
			return "redirect:/order";
		}
		model.addAttribute("pageTitle", "Receipt");
		return "order-confirmation";
	}

	private Map<Long, Integer> parseQuantities(Map<String, String> formData) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		formData.forEach((key, value) -> {
			if (key.startsWith("drink_") && value != null && !value.isBlank()) {
				Long drinkId = Long.parseLong(key.substring(6));
				int quantity = Integer.parseInt(value);
				if (quantity > 0) {
					quantities.put(drinkId, quantity);
				}
			}
		});
		return quantities;
	}

	private BigDecimal calculateRequestedTotal(Long branchId, Map<Long, Integer> quantities) {
		Map<Long, StockLevel> stockByDrink = stockService.getStockForBranch(branchId).stream()
				.collect(java.util.stream.Collectors.toMap(stock -> stock.getDrink().getId(), stock -> stock));

		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
			StockLevel stockLevel = stockByDrink.get(entry.getKey());
			if (stockLevel == null) {
				throw new IllegalArgumentException("Drink not found for the selected branch.");
			}
			total = total.add(stockLevel.getDrink().getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
		}
		return total;
	}

	private List<Branch> getCustomerBranches(List<Branch> allBranches) {
		List<Branch> filteredBranches = allBranches.stream()
				.filter(branch -> CUSTOMER_BRANCH_ORDER.contains(branch.getName()))
				.sorted(Comparator.comparingInt(branch -> CUSTOMER_BRANCH_ORDER.indexOf(branch.getName())))
				.toList();
		return filteredBranches.isEmpty() ? allBranches : filteredBranches;
	}

	private Branch findCustomerBranch(Long branchId) {
		return getCustomerBranches(stockService.getAllBranches()).stream()
				.filter(branch -> branch.getId().equals(branchId))
				.findFirst()
				.orElse(null);
	}

	private String getBranchLabel(Branch branch) {
		if (branch == null) {
			return "";
		}
		if ("Headquarters".equalsIgnoreCase(branch.getName())) {
			return "NAIROBI (HQ)";
		}
		return branch.getName().toUpperCase();
	}
}
