package com.example.secure.product;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductService {
    private final Map<String, Product> productRepo = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(3); // Start after initial data

    public ProductService() {
        // Initial dummy data for users "user1" and "admin"
        productRepo.put("p001", new Product("p001", "Widget A", "User1's product", "user1", 19.99, 10.00));
        productRepo.put("p002", new Product("p002", "Gadget B", "Admin's product", "admin", 99.99, 50.00));
        productRepo.put("p003", new Product("p003", "Thing C", "Another User1 product", "user1", 5.00, 2.50));
    }

    public List<Product> findAll() {
        return new ArrayList<>(productRepo.values());
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(productRepo.get(id));
    }

    public Product save(ProductCreateDTO createDto, String currentUserId) {
        String newId = "p" + idCounter.incrementAndGet();

        // API3: Map DTO to internal Model, preventing unauthorized field setting
        Product newProduct = new Product(
                newId,
                createDto.getName(),
                createDto.getDescription(),
                currentUserId, // OwnerId is set by the system based on the authenticated user
                createDto.getRetailPrice(),
                createDto.getRetailPrice() * 0.5 // Internal cost logic
        );
        productRepo.put(newId, newProduct);
        return newProduct;
    }

    public Product update(String id, ProductCreateDTO updateDto) {
        Product product = productRepo.get(id);
        if (product != null) {
            // Update the fields allowed by the DTO.
            product.setName(updateDto.getName());
            product.setDescription(updateDto.getDescription());
            product.setRetailPrice(updateDto.getRetailPrice());
        }
        return product;
    }

    public boolean delete(String id) {
        return productRepo.remove(id) != null;
    }
}
