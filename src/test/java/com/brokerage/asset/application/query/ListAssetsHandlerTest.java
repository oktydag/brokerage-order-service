package com.brokerage.asset.application.query;

import com.brokerage.asset.application.AssetView;
import com.brokerage.asset.domain.Asset;
import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.infrastructure.AssetQueryRepository;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.Reservation;
import com.brokerage.common.web.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAssetsHandlerTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");

    @Mock
    private AssetQueryRepository assets;

    @Test
    void reportsSizeUsableSizeAndWhatIsReserved() {
        Portfolio portfolio = Portfolio.empty(CUSTOMER);
        portfolio.deposit(AssetName.TRY, Amount.of(100_000));
        portfolio.reserve(new Reservation(AssetName.TRY, Amount.of(30_000)));
        Asset holding = portfolio.holding(AssetName.TRY).orElseThrow();
        Pageable pageable = PageRequest.of(0, 50);
        when(assets.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(holding), pageable, 1));

        PageResponse<AssetView> page = new ListAssetsHandler(assets)
                .handle(new ListAssetsQuery(CUSTOMER, null, false, pageable));

        assertThat(page.content()).singleElement().satisfies(view -> {
            assertThat(view.customerId()).isEqualTo("CUST-1");
            assertThat(view.assetName()).isEqualTo("TRY");
            assertThat(view.size()).isEqualByComparingTo("100000");
            assertThat(view.usableSize()).isEqualByComparingTo("70000");
            assertThat(view.reservedSize()).isEqualByComparingTo("30000");
        });
    }
}
