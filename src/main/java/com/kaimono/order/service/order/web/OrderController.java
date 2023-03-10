package com.kaimono.order.service.order.web;

import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderServicer) {
        this.orderService = orderServicer;
    }

    @GetMapping
    public Flux<Order> getAllOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getAllOrders(jwt.getSubject());
    }

    @PostMapping
    public Mono<Order> submitOrder(@RequestBody @Valid Mono<OrderRequest> orderRequest) {
        return orderRequest.flatMap(request ->
                orderService.submitOrder(request.isbn(), request.quantity()))
                .onErrorResume(WebExchangeBindException.class, Mono::error);
    }

}
