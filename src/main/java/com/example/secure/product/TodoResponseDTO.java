package com.example.secure.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

class TodoResponseDTO {
    @NotNull
    private Integer userId;
    @NotNull
    private Integer id;
    @NotBlank
    private String title;
    @NotNull
    private Boolean completed;

    // Required by Jackson/ObjectMapper
    public TodoResponseDTO() {
    }

    // Getters and Setters (omitted for brevity, but necessary for real usage)

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return String.format("[External Data] ID: %d, UserID: %d, Title: '%s', Completed: %b", id, userId, title, completed);
    }
}
