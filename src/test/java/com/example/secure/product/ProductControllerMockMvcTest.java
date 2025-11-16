package com.example.secure.product;

import com.example.secure.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * =================================================================
 * Automated Security Integration Tests for ProductController
 * =================================================================
 * This test class verifies Authentication (API2), Function-Level Authorization (API5),
 * and Object-Level Authorization (API1) directly against the controller endpoints.
 * It uses Spring's MockMvc for simulating HTTP requests.
 */
@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, ProductService.class}) // Import SecurityConfig and ProductService for full context
public class ProductControllerMockMvcTest {

    // --- Test Data ---
    private final String user1 = "user1";
    private final String pass1 = "password";
    private final String admin = "admin";
    private final String passAdmin = "adminpass";
    private final String productOwnedByUser1 = "p001"; // Owned by user1
    private final String productOwnedByAdmin = "p002"; // Owned by admin
    private final ProductCreateDTO validUpdateDto = new ProductCreateDTO();
    private final ProductCreateDTO validCreateDto = new ProductCreateDTO();
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    public ProductControllerMockMvcTest() {
        // Setup DTO for updating existing products
        validUpdateDto.setName("Updated Test Name");
        validUpdateDto.setDescription("New description");
        validUpdateDto.setRetailPrice(25.00);

        // Setup DTO for creating new products
        validCreateDto.setName("New Product Test");
        validCreateDto.setDescription("A brand new item");
        validCreateDto.setRetailPrice(50.00);
    }

    // ===============================================================
    // API2: Broken Authentication Tests (401 Unauthorized)
    // ===============================================================

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // Attempt to create a product without credentials
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateDto)))
                .andExpect(status().isUnauthorized()); // API2 Check (Failure)
    }

    @Test
    void userShouldBeAllowedToGetAllProducts() throws Exception {
        // user1 (ROLE_USER) attempts to read all products (Auth success)
        mockMvc.perform(get("/api/v1/products")
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isOk()) // API2 Check (Success)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void userShouldBeAllowedToGetSingleProduct() throws Exception {
        // user1 (ROLE_USER) attempts to read a single product (Auth success)
        mockMvc.perform(get("/api/v1/products/" + productOwnedByUser1)
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isOk()) // API2/API5 Check (Success)
                .andExpect(jsonPath("$.id").value(productOwnedByUser1));
    }


    // ===============================================================
    // API5: Broken Function Level Authorization (BFLA) Tests
    // ===============================================================

    @Test
    void userShouldBeForbiddenToDelete() throws Exception {
        // user1 (ROLE_USER) attempts to delete (requires ROLE_ADMIN)
        mockMvc.perform(delete("/api/v1/products/" + productOwnedByUser1)
                        .with(httpBasic(user1, pass1)))
                .andExpect(status().isForbidden()); // API5 Check (Failure)
    }

    @Test
    void adminShouldBeAllowedToDelete() throws Exception {
        // admin (ROLE_ADMIN) attempts to delete (allowed)
        mockMvc.perform(delete("/api/v1/products/" + productOwnedByAdmin)
                        .with(httpBasic(admin, passAdmin)))
                .andExpect(status().isNoContent()); // API5 Check (Success)
    }

    @Test
    void userShouldBeAllowedToCreateProduct() throws Exception {
        // user1 (ROLE_USER) attempts to create a product (allowed)
        mockMvc.perform(post("/api/v1/products")
                        .with(httpBasic(user1, pass1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto)))
                .andExpect(status().isCreated()) // API5 Check (Success - 201 Created)
                .andExpect(jsonPath("$.name").value(validCreateDto.getName()));
    }


    // ===============================================================
    // API1: Broken Object Level Authorization (BOLA) Tests
    // ===============================================================

    @Test
    void userShouldBeForbiddenToUpdateOthersProduct() throws Exception {
        // user1 attempts to update p002 (which is owned by admin)
        mockMvc.perform(put("/api/v1/products/" + productOwnedByAdmin)
                        .with(httpBasic(user1, pass1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateDto)))
                .andExpect(status().isForbidden()); // API1 BOLA Check (Failure)
    }

    @Test
    void userShouldBeAllowedToUpdateOwnProduct() throws Exception {
        // user1 attempts to update p001 (which is owned by user1)
        mockMvc.perform(put("/api/v1/products/" + productOwnedByUser1)
                        .with(httpBasic(user1, pass1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateDto)))
                .andExpect(status().isOk()); // API1 BOLA Check (Success)
    }

    // ===============================================================
    // API8: Injection/Input Validation Test
    // ===============================================================

    @Test
    void userShouldReceiveBadRequestOnInvalidInput() throws Exception {
        ProductCreateDTO invalidDto = new ProductCreateDTO();
        invalidDto.setName("S"); // Too short (< 3 chars)
        invalidDto.setDescription("Valid description");
        invalidDto.setRetailPrice(10.00);

        // user1 attempts to create a product with invalid data
        mockMvc.perform(post("/api/v1/products")
                        .with(httpBasic(user1, pass1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()); // API8 Input Validation Check (Failure)
    }
}