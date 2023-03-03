package com.kaimono.order.service.order.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(

        @NotBlank(message = "The book ISBN must be defined.")
        String isbn,

        @NotNull(message = "The book quantity must be defined.")
        @Positive(message = "The book quantity must be positive.")
        Integer quantity

) { }
