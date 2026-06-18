package dto;

import java.math.BigDecimal;
import java.util.List;

public record BookingReceipt(
        String bookingId,
        List<Seat> seats,
        BigDecimal totalAmount,
        String status
) {}
