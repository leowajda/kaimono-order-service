package com.kaimono.order.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kaimono.order.service.book.Book;
import com.kaimono.order.service.book.BookClient;
import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderStatus;
import com.kaimono.order.service.order.web.OrderRequest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import junit.aggregator.book.CsvToBook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KaimonoOrderServiceApplicationTests {

    private static KeycloakToken bjornTokens;
    private static KeycloakToken isabelleTokens;

    @Container
    private static final PostgreSQLContainer<?> postgresql =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Container
    private static final KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:19.0")
            .withRealmImportFile("test-realm-config.json");

    @BeforeAll
    public static void generateAccessTokens() {
        WebClient webClient = WebClient.builder()
                .baseUrl(keycloakContainer.getAuthServerUrl() + "realms/kaimono/protocol/openid-connect/token")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        isabelleTokens = authenticateWith("isabelle", "password", webClient);
        bjornTokens = authenticateWith("bjorn", "password", webClient);
    }

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private BookClient bookClient;

    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenGetOrdersThenReturn(@CsvToBook Book book) {
        given(bookClient.getBookByIsbn(book.isbn()))
                .willReturn(Mono.just(book));

        var request = new OrderRequest(book.isbn(), 1);
        webClient
                .post()
                    .uri("/orders")
                        .headers(header -> header.setBearerAuth(isabelleTokens.accessToken()))
                            .bodyValue(request)
                .exchange()
                .expectStatus()
                    .isOk()
                .expectBody(Order.class)
                    .value(order -> assertThat(order).isNotNull());

        webClient
                .get()
                    .uri("/orders")
                        .headers(header -> header.setBearerAuth(isabelleTokens.accessToken()))
                .exchange()
                .expectStatus()
                    .isOk()
                .expectBodyList(Order.class)
                    .value(orders -> assertThat(orders.stream().map(Order::bookIsbn).collect(toSet()))
                                    .contains(book.isbn()));
    }


    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenPostRequestAndBookExistsThenOrderAccepted(@CsvToBook Book book) {
        given(bookClient.getBookByIsbn(book.isbn()))
                .willReturn(Mono.just(book));

        var request = new OrderRequest(book.isbn(), 3);
        var submittedOrder = webClient
                .post()
                    .uri("/orders")
                        .headers(header -> header.setBearerAuth(bjornTokens.accessToken()))
                            .bodyValue(request)
                .exchange()
                .expectStatus()
                    .isOk()
                .expectBody(Order.class)
                .returnResult()
                    .getResponseBody();


        assertThat(submittedOrder).isNotNull();
        assertThat(submittedOrder.bookIsbn()).isEqualTo(request.isbn());
        assertThat(submittedOrder.quantity()).isEqualTo(request.quantity());
        assertThat(submittedOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
        assertThat(submittedOrder.bookPrice()).isEqualTo(book.price());
        assertThat(submittedOrder.status()).isEqualTo(OrderStatus.ACCEPTED);
    }

    @ParameterizedTest
    @CsvSource("1234567895, Thus Spoke Zarathustra, Friedrich Nietzsche, 9.90")
    void whenPostRequestAndBookNotExistsThenOrderRejected(@CsvToBook Book book) {
        given(bookClient.getBookByIsbn(book.isbn()))
                .willReturn(Mono.empty());

        var request = new OrderRequest(book.isbn(), 3);
        var submittedOrder = webClient
                .post()
                    .uri("/orders")
                        .headers(header -> header.setBearerAuth(isabelleTokens.accessToken()))
                            .bodyValue(request)
                .exchange()
                .expectStatus()
                    .isOk()
                .expectBody(Order.class)
                .returnResult()
                    .getResponseBody();

        assertThat(submittedOrder).isNotNull();
        assertThat(submittedOrder.bookIsbn()).isEqualTo(request.isbn());
        assertThat(submittedOrder.quantity()).isEqualTo(request.quantity());
        assertThat(submittedOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }


    @DynamicPropertySource
    private static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloakContainer.getAuthServerUrl() + "realms/kaimono");
    }

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

    private record KeycloakToken(String accessToken) {
        @JsonCreator
        private KeycloakToken(@JsonProperty("access_token") String accessToken) {
            this.accessToken = accessToken;
        }
    }

    private static KeycloakToken authenticateWith(String username, String password, WebClient webClient) {
        return webClient
                .post()
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "kaimono-test")
                        .with("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(KeycloakToken.class)
                .block();
    }

}
