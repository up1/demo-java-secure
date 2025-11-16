package com.example.secure.product;

public class Product {
    private String id;
    private String name;
    private String description;
    private String ownerId;
    private double retailPrice; // Publicly visible price
    private double costPrice;   // Internal/Sensitive field - DO NOT EXPOSE

    public Product(String id, String name, String description, String ownerId, double retailPrice, double costPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.retailPrice = retailPrice;
        this.costPrice = costPrice;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public double getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(double retailPrice) {
        this.retailPrice = retailPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }
}
