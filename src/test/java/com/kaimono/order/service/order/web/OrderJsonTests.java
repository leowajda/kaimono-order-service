package com.kaimono.order.service.order.web;

import junit.aggregator.order.CsvToOrder;
import com.kaimono.order.service.order.domain.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
public class OrderJsonTests {

    @Autowired
    private JacksonTester<Order> json;

    @ParameterizedTest
    @CsvSource("1234567890, Thus Spoke Zarathustra, 9.90, 1, ACCEPTED")
    void testSerialize(@CsvToOrder Order order) throws Exception {
        var payload = json.write(order);


        assertThat(payload).extractingJsonPathStringValue("@.bookIsbn")
                .isEqualTo(order.bookIsbn());


        assertThat(payload).extractingJsonPathStringValue("@.bookName")
                .isEqualTo(order.bookName());


        assertThat(payload).extractingJsonPathNumberValue("@.bookPrice")
                .isEqualTo(order.bookPrice());


        assertThat(payload).extractingJsonPathNumberValue("@.quantity")
                .isEqualTo(order.quantity());


        assertThat(payload).extractingJsonPathStringValue("@.status")
                .isEqualTo(order.status().toString());

    }

}
