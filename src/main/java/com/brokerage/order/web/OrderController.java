package com.brokerage.order.web;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.web.PageResponse;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.application.command.CancelOrderCommand;
import com.brokerage.order.application.command.CancelOrderHandler;
import com.brokerage.order.application.command.PlaceOrderCommand;
import com.brokerage.order.application.command.PlaceOrderHandler;
import com.brokerage.order.application.query.GetOrderHandler;
import com.brokerage.order.application.query.GetOrderQuery;
import com.brokerage.order.application.query.ListOrdersHandler;
import com.brokerage.order.application.query.ListOrdersQuery;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.order.domain.valueobjects.OrderStatus;
import com.brokerage.security.AccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Submit, list and cancel stock orders")
public class OrderController {

    private final PlaceOrderHandler placeOrder;
    private final CancelOrderHandler cancelOrder;
    private final ListOrdersHandler listOrders;
    private final GetOrderHandler getOrder;
    private final AccessPolicy accessPolicy;

    public OrderController(PlaceOrderHandler placeOrder, CancelOrderHandler cancelOrder,
                           ListOrdersHandler listOrders, GetOrderHandler getOrder,
                           AccessPolicy accessPolicy) {
        this.placeOrder = placeOrder;
        this.cancelOrder = cancelOrder;
        this.listOrders = listOrders;
        this.getOrder = getOrder;
        this.accessPolicy = accessPolicy;
    }

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderView> place(@Valid @RequestBody PlaceOrderRequest request,
                                           UriComponentsBuilder uriBuilder) {
        CustomerId customerId = accessPolicy.currentScope()
                .resolveTarget(CustomerId.ofNullable(request.customerId()));

        OrderView order = placeOrder.handle(new PlaceOrderCommand(
                customerId,
                AssetName.of(request.assetName()),
                request.orderSide(),
                Amount.of(request.size()),
                Amount.of(request.price())));

        URI location = uriBuilder.path("/api/v1/orders/{id}").buildAndExpand(order.id()).toUri();
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping
    @Operation(summary = "List orders for a customer within a date range")
    public PageResponse<OrderView> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) List<OrderStatus> status,
            @RequestParam(required = false) String assetName,
            @RequestParam(required = false) OrderSide orderSide,
            @ParameterObject @PageableDefault(size = 20, sort = "createDate",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        CustomerId target = accessPolicy.currentScope()
                .resolveTarget(CustomerId.ofNullable(customerId));

        return listOrders.handle(new ListOrdersQuery(
                target,
                from,
                to,
                status == null ? Set.of() : Set.copyOf(status),
                assetName == null ? null : AssetName.of(assetName),
                orderSide,
                pageable));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Fetch a single order")
    public OrderView get(@PathVariable UUID orderId) {
        return getOrder.handle(new GetOrderQuery(orderId, accessPolicy.currentScope()));
    }

    @DeleteMapping("/{orderId}")
    @Operation(summary = "Cancel a pending order")
    public OrderView cancel(@PathVariable UUID orderId) {
        return cancelOrder.handle(new CancelOrderCommand(orderId, accessPolicy.currentScope()));
    }
}
