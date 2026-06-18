package enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum SeatType {
    VIP(new BigDecimal("1000.00")),
    REGULAR(new BigDecimal("500.00"));

    private final BigDecimal price;
}
