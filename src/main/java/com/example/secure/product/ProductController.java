package com.example.secure.product;

import com.example.secure.global.RateLimitingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;
    private final RateLimitingService rateLimitingService;

    public ProductController(ProductService productService, RateLimitingService rateLimitingService) {
        this.productService = productService;
        this.rateLimitingService = rateLimitingService;
    }

    /**
     * Retrieves all products. Accessible to all authenticated users.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {

        // Add rate limit !!
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitingService.allowRequest(currentUserId)) {
            // Respond with 429 Too Many Requests if the limit is exceeded
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .build();
        }

        return ResponseEntity.ok(productService.findAll().stream()
                .map(ProductResponseDTO::new) // API3: Use DTO for output
                .collect(Collectors.toList()));
    }

    /**
     * Retrieves a single product.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')") // BFLA (API5)
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable String id) {
        return productService.findById(id)
                .map(ProductResponseDTO::new) // API3: Use DTO for output
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Creates a new product.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('USER')") // BFLA (API5)
    public ProductResponseDTO createProduct(@Valid @RequestBody ProductCreateDTO createDto) {
        // API2/API1: Get current user context
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        Product newProduct = productService.save(createDto, currentUserId);

        return new ProductResponseDTO(newProduct);
    }

    /**
     * Updates an existing product.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')") // BFLA (API5)
    public ResponseEntity<ProductResponseDTO> updateProduct(@PathVariable String id, @Valid @RequestBody ProductCreateDTO updateDto) {
        Product product = productService.findById(id).orElse(null);
        if (product == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // --- BOLA Check (API1: Broken Object Level Authorization) ---
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!product.getOwnerId().equals(currentUserId)) {
            // Forbidden: The authenticated user is trying to modify a resource they do not own.
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        // -----------------------------------------------------------

        Product updatedProduct = productService.update(id, updateDto);

        return ResponseEntity.ok(new ProductResponseDTO(updatedProduct));
    }

    /**
     * Deletes a product.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // BFLA (API5) - Only ADMIN can delete
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productService.findById(id).isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        productService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
