package com.finance.manager.config;

import com.finance.manager.model.Category;
import com.finance.manager.model.TransactionType;
import com.finance.manager.repository.CategoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public DataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefault("Salary", TransactionType.INCOME);
        seedDefault("Food", TransactionType.EXPENSE);
        seedDefault("Rent", TransactionType.EXPENSE);
        seedDefault("Transportation", TransactionType.EXPENSE);
        seedDefault("Entertainment", TransactionType.EXPENSE);
        seedDefault("Healthcare", TransactionType.EXPENSE);
        seedDefault("Utilities", TransactionType.EXPENSE);
    }

    private void seedDefault(String name, TransactionType type) {
        if (!categoryRepository.existsByNameAndIsDefaultTrue(name)) {
            categoryRepository.save(new Category(name, type, true, null));
        }
    }
}
