package com.kaimono.order.service.book;

public record Book(
        String isbn,
        String title,
        String author,
        Double price
) { }
