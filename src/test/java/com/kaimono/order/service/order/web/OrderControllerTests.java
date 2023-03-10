package com.kaimono.order.service.order.web;

import com.kaimono.order.service.config.SecurityConfig;
import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderService;
import com.kaimono.order.service.order.domain.OrderStatus;
import junit.aggregator.order.request.CsvToOrderRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static com.kaimono.order.service.order.domain.OrderService.buildRejectedOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
public class OrderControllerTests {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ReactiveJwtDecoder reactiveJwtDecoder;

    @ParameterizedTest
    @WithMockUser(roles = "employee")
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
                .expectStatus()
                    .isOk()
                .expectBody(Order.class)
                    .value(order -> assertThat(order.status()).isNotNull().isEqualTo(OrderStatus.REJECTED));
    }

}
