package dto;

import enums.SeatType;

import java.math.BigDecimal;

public record Seat(
        String row,
        int number,
        SeatType type
) {
    public BigDecimal price() {
        return type.getPrice();
    }

    public static Seat regularSeat(final String row, int number) {
        return new Seat(row, number, SeatType.REGULAR);
    }

    public static Seat vipSeat(final String row, int number) {
        return new Seat(row, number, SeatType.VIP);
    }
}
