package com.drinksco.config;

import com.drinksco.model.Branch;
import com.drinksco.model.Drink;
import com.drinksco.repository.BranchRepository;
import com.drinksco.repository.DrinkRepository;
import com.drinksco.service.CustomerCatalogService;
import com.drinksco.service.StockService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

	private final BranchRepository branchRepository;
	private final DrinkRepository drinkRepository;
	private final StockService stockService;
	private final CustomerCatalogService customerCatalogService;

	@Bean
	@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
	CommandLineRunner loadSampleData() {
		return args -> {
			Map<Branch, Integer> branches = new LinkedHashMap<>();
			customerCatalogService.getSeedBranches().forEach(seed ->
					branches.put(ensureBranch(seed.name(), seed.location()), seed.stockQuantity()));

			var drinks = customerCatalogService.getSeedDrinks().stream()
					.map(seed -> ensureDrink(seed.name(), seed.category(), seed.price()))
					.toList();

			for (Map.Entry<Branch, Integer> branchEntry : branches.entrySet()) {
				for (Drink drink : drinks) {
					stockService.seedStock(branchEntry.getKey(), drink, branchEntry.getValue());
				}
			}
		};
	}

	private Branch ensureBranch(String name, String location) {
		return branchRepository.findByNameIgnoreCase(name)
				.orElseGet(() -> branchRepository.save(Branch.builder()
						.name(name)
						.location(location)
						.build()));
	}

	private Drink ensureDrink(String name, String category, java.math.BigDecimal price) {
		return drinkRepository.findByNameIgnoreCase(name)
				.orElseGet(() -> drinkRepository.save(Drink.builder()
						.name(name)
						.category(category)
						.price(price)
						.build()));
	}
}
