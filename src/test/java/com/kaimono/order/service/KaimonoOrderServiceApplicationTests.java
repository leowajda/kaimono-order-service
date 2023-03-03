package com.kaimono.order.service;

import com.kaimono.order.service.book.Book;
import com.kaimono.order.service.book.BookClient;
import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderStatus;
import com.kaimono.order.service.order.web.OrderRequest;
import junit.aggregator.book.CsvToBook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KaimonoOrderServiceApplicationTests {

    @Container
    private static final PostgreSQLContainer<?> postgresql =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private BookClient bookClient;

    @DynamicPropertySource
    private static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%s/%s",
                        postgresql.getHost(),
                        postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                        postgresql.getDatabaseName())
        );
    }

    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenGetOrdersThenReturn(@CsvToBook Book book) {
        var isbn = book.isbn();

        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));

        var orderRequest = new OrderRequest(isbn, 1);
        var expectedOrder = webClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult().getResponseBody();
        assertThat(expectedOrder).isNotNull();

        webClient.get().uri("/orders")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> {
                    assertThat(orders.stream().filter(order -> order.bookIsbn().equals(isbn)).findAny()).isNotEmpty();
                });
    }


    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenPostRequestAndBookExistsThenOrderAccepted(@CsvToBook Book book) {
        var isbn = book.isbn();
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.just(book));

        var orderRequest = new OrderRequest(isbn, 3);
        var createdOrder = webClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
        assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenPostRequestAndBookNotExistsThenOrderRejected(@CsvToBook Book book) {
        var isbn = book.isbn();
        given(bookClient.getBookByIsbn(isbn)).willReturn(Mono.empty());

        var orderRequest = new OrderRequest(isbn, 3);
        var createdOrder = webClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class)
                .returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }

}
