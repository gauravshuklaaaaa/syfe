package com.finance.manager.repository;

import com.finance.manager.model.Category;
import com.finance.manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsDefaultTrue();
    List<Category> findByUserAndIsDefaultFalse(User user);
    Optional<Category> findByNameAndIsDefaultTrue(String name);
    Optional<Category> findByNameAndUser(String name, User user);
    boolean existsByNameAndUser(String name, User user);
    boolean existsByNameAndIsDefaultTrue(String name);
}
