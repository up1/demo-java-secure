package com.example.secure.product;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.secure.global.RateLimitingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    // Add log
    private static final Logger SECURITY_LOGGER = LoggerFactory.getLogger(ProductController.class);

    // Add deprecated api
    private static final String DEPRECATION_HEADER = "X-API-Deprecation-Notice";
    private static final String DEPRECATION_MESSAGE = "This endpoint is deprecated and will be retired. Please migrate to /api/v2/products.";


    private final ProductService productService;
    private final RateLimitingService rateLimitingService;

    public ProductController(ProductService productService, RateLimitingService rateLimitingService) {
        this.productService = Objects.requireNonNull(productService, "productService cannot be null");
        this.rateLimitingService = Objects.requireNonNull(rateLimitingService, "rateLimitingService cannot be null");
    }

    /**
     * Retrieves all products. Accessible to all authenticated users.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {

        // Add rate limit !!
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!rateLimitingService.allowRequest(currentUserId)) {
            // Add log
            SECURITY_LOGGER.warn("SECURITY-API4-FAIL: Global rate limit exceeded for user: {}", currentUserId);

            // Respond with 429 Too Many Requests if the limit is exceeded
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .header(DEPRECATION_HEADER, DEPRECATION_MESSAGE)
                    .build();
        }

        // API3 Defense Check (Read): Ensure sensitive fields are filtered out by the DTO.
        return ResponseEntity.ok()
                .header(DEPRECATION_HEADER, DEPRECATION_MESSAGE)
                .body(productService.findAll().stream()
                        .map(ProductResponseDTO::new)
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
            SECURITY_LOGGER.error("SECURITY-API1-BOLA: User {} attempted unauthorized update on product ID {} owned by {}",
                    currentUserId, id, product.getOwnerId());
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

        // Add log
        SECURITY_LOGGER.info("SECURITY-API5-AUDIT: Admin {} successfully deleted product ID {}",
                SecurityContextHolder.getContext().getAuthentication().getName(), id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
