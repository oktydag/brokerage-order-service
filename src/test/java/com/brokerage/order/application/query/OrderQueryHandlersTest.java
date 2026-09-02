package com.brokerage.order.application.query;

import com.brokerage.common.domain.ForbiddenException;
import com.brokerage.common.domain.valueobjects.AccessScope;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.web.PageResponse;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import com.brokerage.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryHandlersTest {

    @Mock
    private OrderQueryRepository orders;

    private ListOrdersHandler listOrders;
    private GetOrderHandler getOrder;
    private Order order;

    @BeforeEach
    void setUp() {
        listOrders = new ListOrdersHandler(orders);
        getOrder = new GetOrderHandler(orders);
        order = Fixtures.buyOrder();
    }

    @Test
    void listReturnsAPagedEnvelopeOfViews() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orders.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        PageResponse<OrderView> page = listOrders.handle(new ListOrdersQuery(
                Fixtures.CUSTOMER, null, null, Set.of(), null, null, pageable));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.last()).isTrue();
        assertThat(page.content()).singleElement()
                .extracting(OrderView::id).isEqualTo(order.getId());
    }

    @Test
    void getReturnsASingleOrder() {
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        OrderView view = getOrder.handle(new GetOrderQuery(order.getId(), AccessScope.unrestricted()));

        assertThat(view.id()).isEqualTo(order.getId());
        assertThat(view.customerId()).isEqualTo("CUST-1");
    }

    @Test
    void getReportsAnUnknownOrder() {
        UUID unknown = UUID.randomUUID();
        when(orders.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOrder.handle(new GetOrderQuery(unknown, AccessScope.unrestricted())))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getRefusesAnotherCustomersOrder() {
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> getOrder.handle(new GetOrderQuery(order.getId(),
                AccessScope.of(CustomerId.of("CUST-2")))))
                .isInstanceOf(ForbiddenException.class);
    }
}
