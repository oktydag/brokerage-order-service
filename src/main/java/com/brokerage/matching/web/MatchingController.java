package com.brokerage.matching.web;

import com.brokerage.matching.application.MatchReport;
import com.brokerage.matching.application.command.MatchOrdersCommand;
import com.brokerage.matching.application.command.MatchOrdersHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Matching", description = "Operator-only execution of pending orders")
public class MatchingController {

    private final MatchOrdersHandler matchOrders;

    public MatchingController(MatchOrdersHandler matchOrders) {
        this.matchOrders = matchOrders;
    }

    @PostMapping("/match")
    @Operation(summary = "Match a set of pending orders")
    public MatchReport match(@Valid @RequestBody MatchOrdersRequest request) {
        return matchOrders.handle(new MatchOrdersCommand(request.orderIds()));
    }
}
