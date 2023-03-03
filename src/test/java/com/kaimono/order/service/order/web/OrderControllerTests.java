package com.kaimono.order.service.order.web;

import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderService;
import com.kaimono.order.service.order.domain.OrderStatus;
import junit.aggregator.order.request.CsvToOrderRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static com.kaimono.order.service.order.domain.OrderService.buildRejectedOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
public class OrderControllerTests {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private OrderService orderService;

    @ParameterizedTest
    @CsvSource("1234567890, 10")
    public void whenBookNotAvailableThenRejectOrder(@CsvToOrderRequest OrderRequest request) {
        var expectedOrder = buildRejectedOrder(request.isbn(), request.quantity());

        given(orderService.submitOrder(request.isbn(), request.quantity()))
                .willReturn(Mono.just(expectedOrder));

        webClient
                .post()
                .uri("/orders")
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).value(order -> {
                        assertThat(order).isNotNull();
                        assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
                    }
                );
    }

}
