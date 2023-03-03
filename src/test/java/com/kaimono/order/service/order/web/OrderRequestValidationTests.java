package com.kaimono.order.service.order.web;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import junit.aggregator.order.request.CsvToOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderRequestValidationTests {

    private static Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @CsvSource("1234567890, 10")
    public void whenAllFieldsCorrectThenValidationSucceeds(@CsvToOrderRequest OrderRequest request) {
        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "N/A", value = "N/A, 1")
    public void whenIsbnNotDefinedThenValidationFails(@CsvToOrderRequest OrderRequest request) {
        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book ISBN must be defined.");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "N/A", value = "1234567890, N/A")
    public void whenQuantityIsNotDefinedThenValidationFails(@CsvToOrderRequest OrderRequest request) {
        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book quantity must be defined.");
    }

    @ParameterizedTest
    @CsvSource("1234567890, -1")
    public void whenQuantityIsNegativeThenValidationFails(@CsvToOrderRequest OrderRequest request) {
        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("The book quantity must be positive.");
    }


}
