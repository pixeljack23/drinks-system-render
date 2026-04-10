package com.drinksco.service;

import com.drinksco.model.Branch;
import com.drinksco.model.Drink;
import com.drinksco.model.StockLevel;
import com.drinksco.repository.BranchRepository;
import com.drinksco.repository.DrinkRepository;
import com.drinksco.repository.StockLevelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockService {

	private final StockLevelRepository stockLevelRepository;
	private final BranchRepository branchRepository;
	private final DrinkRepository drinkRepository;

	public List<StockLevel> getAllStockLevels() {
		return stockLevelRepository.findAllByOrderByBranchNameAscDrinkNameAsc();
	}

	public List<StockLevel> getStockForBranch(Long branchId) {
		return stockLevelRepository.findAllByBranchIdOrderByDrinkNameAsc(branchId);
	}

	@Transactional
	public void seedStock(Branch branch, Drink drink, int quantity) {
		stockLevelRepository.findByBranchIdAndDrinkId(branch.getId(), drink.getId())
				.orElseGet(() -> stockLevelRepository.save(StockLevel.builder()
						.branch(branch)
						.drink(drink)
						.quantity(quantity)
						.build()));
	}

	@Transactional
	public void adjustStock(Long branchId, Long drinkId, int quantityDelta) {
		StockLevel stockLevel = stockLevelRepository.findByBranchIdAndDrinkId(branchId, drinkId)
				.orElseThrow(() -> new IllegalArgumentException("Stock record not found."));
		int updatedQuantity = stockLevel.getQuantity() + quantityDelta;
		if (updatedQuantity < 0) {
			throw new IllegalArgumentException("Insufficient stock for " + stockLevel.getDrink().getName());
		}
		stockLevel.setQuantity(updatedQuantity);
	}

	@Transactional
	public void restock(Long branchId, Long drinkId, Integer quantity) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("Restock quantity must be greater than zero.");
		}
		adjustStock(branchId, drinkId, quantity);
	}

	public List<Branch> getAllBranches() {
		return branchRepository.findAll();
	}

	public List<Drink> getAllDrinks() {
		return drinkRepository.findAll();
	}
}
