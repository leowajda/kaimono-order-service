package com.kaimono.order.service.order.domain;

import com.kaimono.order.service.config.DataConfig;
import junit.aggregator.order.CsvToOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Testcontainers
@Import(DataConfig.class)
public class OrderRepositoryTests {

    @Container
    private static final PostgreSQLContainer<?> postgresql =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.4"));

    @Autowired
    private OrderRepository orderRepository;

    @ParameterizedTest
    @CsvSource("1234567890, Thus Spoke Zarathustra, 9.90, 1, REJECTED")
    public void createRejectedOrder(@CsvToOrder Order order) {
        var savedOrder = orderRepository.save(order);
        StepVerifier.create(savedOrder)
                .expectNextMatches(incomingOrder ->
                        incomingOrder.status().equals(OrderStatus.REJECTED))
                .verifyComplete();
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

}
