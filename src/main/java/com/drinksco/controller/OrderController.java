package com.drinksco.controller;

import com.drinksco.model.Order;
import com.drinksco.service.OrderService;
import com.drinksco.service.StockService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OrderController {

	private final StockService stockService;
	private final OrderService orderService;

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("pageTitle", "Branch Ordering");
		model.addAttribute("branches", stockService.getAllBranches());
		model.addAttribute("drinks", stockService.getAllDrinks());
		return "customer";
	}

	@PostMapping("/orders")
	public String placeOrder(
			@RequestParam Long branchId,
			@RequestParam(required = false) String customerName,
			@RequestParam Map<String, String> formData,
			Model model,
			RedirectAttributes redirectAttributes) {

		Map<Long, Integer> quantities = new LinkedHashMap<>();
		formData.forEach((key, value) -> {
			if (key.startsWith("drink_") && value != null && !value.isBlank()) {
				Long drinkId = Long.parseLong(key.substring(6));
				quantities.put(drinkId, Integer.parseInt(value));
			}
		});

		try {
			Order order = orderService.placeOrder(branchId, customerName, quantities);
			model.addAttribute("pageTitle", "Order Confirmation");
			model.addAttribute("order", order);
			return "order-confirmation";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/";
		}
	}
}
