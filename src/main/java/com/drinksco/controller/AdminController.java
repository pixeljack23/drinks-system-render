package com.drinksco.controller;

import com.drinksco.model.Order;
import com.drinksco.model.Branch;
import com.drinksco.repository.OrderRepository;
import com.drinksco.service.ReportService;
import com.drinksco.service.StockService;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

	private final StockService stockService;
	private final ReportService reportService;
	private final OrderRepository orderRepository;

	@GetMapping("/admin")
	public String dashboard(Model model) {
		model.addAttribute("pageTitle", "Admin Dashboard");
		model.addAttribute("branches", stockService.getAllBranches());
		model.addAttribute("recentOrders", reportService.getRecentOrders());
		model.addAttribute("totalOrders", reportService.getTotalOrders());
		model.addAttribute("totalRevenue", reportService.getTotalRevenue());
		return "admin/dashboard";
	}

	@GetMapping("/admin/stock")
	public String stock(@RequestParam(required = false) Long branchId, Model model) {
		List<Branch> branches = stockService.getAllBranches();
		Long selectedBranchId = branchId;
		if (selectedBranchId == null && !branches.isEmpty()) {
			selectedBranchId = branches.get(0).getId();
		}

		model.addAttribute("pageTitle", "Stock Levels");
		model.addAttribute("branches", branches);
		model.addAttribute("selectedBranchId", selectedBranchId);
		model.addAttribute("stockLevels", selectedBranchId == null
				? List.of()
				: stockService.getStockForBranch(selectedBranchId));
		return "admin/stock";
	}

	@PostMapping("/admin/stock/restock")
	public String restock(
			@RequestParam Long branchId,
			@RequestParam Long drinkId,
			@RequestParam Integer quantity,
			RedirectAttributes redirectAttributes) {
		try {
			stockService.restock(branchId, drinkId, quantity);
			redirectAttributes.addFlashAttribute("successMessage", "Stock updated successfully.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/admin/stock?branchId=" + branchId;
	}

	@GetMapping("/admin/orders")
	public String orders(Model model) {
		List<Order> allOrders = orderRepository.findAll();
		List<Branch> branches = stockService.getAllBranches();
		
		// Group orders by branch
		List<Order> nairobiOrders = allOrders.stream()
				.filter(o -> o.getBranch() != null && "NAIROBI(HQ)".equalsIgnoreCase(o.getBranch().getName()))
				.collect(Collectors.toList());
		
		List<Order> kisumuOrders = allOrders.stream()
				.filter(o -> o.getBranch() != null && "KISUMU".equalsIgnoreCase(o.getBranch().getName()))
				.collect(Collectors.toList());
		
		List<Order> nakuruOrders = allOrders.stream()
				.filter(o -> o.getBranch() != null && "NAKURU".equalsIgnoreCase(o.getBranch().getName()))
				.collect(Collectors.toList());
		
		List<Order> mombasaOrders = allOrders.stream()
				.filter(o -> o.getBranch() != null && "MOMBASA".equalsIgnoreCase(o.getBranch().getName()))
				.collect(Collectors.toList());
		
		// Calculate total sales
		BigDecimal totalSales = allOrders.stream()
				.map(Order::getTotalAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		model.addAttribute("pageTitle", "Orders");
		model.addAttribute("branches", branches);
		model.addAttribute("nairobiOrders", nairobiOrders);
		model.addAttribute("kisumuOrders", kisumuOrders);
		model.addAttribute("nakuruOrders", nakuruOrders);
		model.addAttribute("mombasaOrders", mombasaOrders);
		model.addAttribute("totalOrders", allOrders.size());
		model.addAttribute("totalSalesAmount", totalSales);
		return "admin/Orders";
	}
}
